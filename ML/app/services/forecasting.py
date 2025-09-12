import pandas as pd
import numpy as np
from sqlalchemy import func
from sqlalchemy.orm import Session
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.metrics import mean_squared_error, mean_absolute_error
from sklearn.preprocessing import StandardScaler
from sklearn.feature_selection import SelectKBest, f_regression
from datetime import datetime
from ..models.database import Order, OrderItem, OrderStatus
import logging

logger = logging.getLogger(__name__)


class Forecasting:
    def __init__(self, db: Session):
        self.db = db
        self.scaler = StandardScaler()
        self.model = None
        self.feature_selector = None
        self.results = {}

    def get_holiday_dates(self) -> list:
        current_year = datetime.now().year
        holidays = []

        for year in range(current_year - 1, current_year + 2):
            holidays.extend([
                datetime(year, 1, 1),
                datetime(year, 11, 24)
            ])

        return holidays

    def get_sales_data(self) -> pd.DataFrame:
        try:
            query = self.db.query(
                func.date(Order.order_date).label('date'),
                func.sum(OrderItem.quantity * OrderItem.unit_price).label('sales'),
                func.count(Order.order_id).label('orders'),
                func.count(func.distinct(Order.customer_id)).label('customers')
            ).join(OrderItem).filter(Order.status == OrderStatus.DELIVERED)

            query = query.group_by(func.date(Order.order_date)).order_by('date')

            results = query.all()

            if not results:
                logger.warning("No sales data found, creating minimal dataset")
                today = datetime.now().date()
                df = pd.DataFrame({
                    'date': [today],
                    'sales': [0.0],
                    'orders': [0],
                    'customers': [0]
                })
                df['date'] = pd.to_datetime(df['date'])
                df = df.set_index('date')
                return df

            data = []
            for result in results:
                row_data = {
                    'date': result.date,
                    'sales': float(result.sales) if result.sales is not None else 0.0,
                    'orders': int(result.orders) if result.orders is not None else 0,
                    'customers': int(result.customers) if result.customers is not None else 0
                }
                data.append(row_data)

            df = pd.DataFrame(data)
            df['date'] = pd.to_datetime(df['date'])
            df = df.set_index('date')

            if len(df) > 1:
                full_range = pd.date_range(start=df.index.min(), end=df.index.max(), freq='D')
                df = df.reindex(full_range, fill_value=0)

            df['sales'] = pd.to_numeric(df['sales'], errors='coerce').fillna(0.0)
            df['orders'] = pd.to_numeric(df['orders'], errors='coerce').fillna(0).astype(int)
            df['customers'] = pd.to_numeric(df['customers'], errors='coerce').fillna(0).astype(int)

            return df

        except Exception as e:
            logger.error(f"Error getting sales data: {e}")
            today = datetime.now().date()
            df = pd.DataFrame({
                'date': [today],
                'sales': [0.0],
                'orders': [0],
                'customers': [0]
            })
            df['date'] = pd.to_datetime(df['date'])
            df = df.set_index('date')
            return df

    def create_features(self, df: pd.DataFrame) -> pd.DataFrame:
        try:
            features = df.copy()

            features['year'] = features.index.year
            features['month'] = features.index.month
            features['day_of_week'] = features.index.dayofweek
            features['day_of_year'] = features.index.dayofyear
            features['quarter'] = features.index.quarter
            features['is_weekend'] = features['day_of_week'].isin([5, 6]).astype(int)

            season_map = {12: 0, 1: 0, 2: 0, 3: 1, 4: 1, 5: 1,
                          6: 2, 7: 2, 8: 2, 9: 3, 10: 3, 11: 3}
            features['season'] = features['month'].map(season_map)

            try:
                holiday_dates = pd.to_datetime(self.get_holiday_dates())
                features['is_holiday'] = features.index.normalize().isin(holiday_dates.normalize()).astype(int)
            except Exception as e:
                logger.warning(f"Error creating holiday features: {e}")
                features['is_holiday'] = 0

            if len(features) > 1:
                features['sales_lag_1'] = features['sales'].shift(1).fillna(0)
            else:
                features['sales_lag_1'] = 0

            if len(features) > 7:
                features['sales_lag_7'] = features['sales'].shift(7).fillna(0)
                features['sales_ma_7'] = features['sales'].rolling(7, min_periods=1).mean()
            else:
                features['sales_lag_7'] = 0
                features['sales_ma_7'] = features['sales']

            if len(features) > 30:
                features['sales_ma_30'] = features['sales'].rolling(30, min_periods=1).mean()
            else:
                features['sales_ma_30'] = features['sales']

            if len(features) > 7:
                sales_shifted = features['sales'].shift(7).fillna(0)
                features['sales_growth_7d'] = np.divide(
                    features['sales'] - sales_shifted,
                    sales_shifted,
                    out=np.zeros_like(features['sales']),
                    where=(sales_shifted != 0)
                )
                features['sales_growth_7d'] = np.where(
                    np.isfinite(features['sales_growth_7d']),
                    features['sales_growth_7d'],
                    0
                )
            else:
                features['sales_growth_7d'] = 0

            features['trend'] = np.arange(len(features))

            features = features.replace([np.inf, -np.inf], 0)

            return features

        except Exception as e:
            logger.error(f"Error creating features: {e}")
            features = df.copy()
            features['year'] = features.index.year
            features['month'] = features.index.month
            features['day_of_week'] = features.index.dayofweek
            features['trend'] = np.arange(len(features))
            return features

    def clean_data(self, df: pd.DataFrame) -> pd.DataFrame:
        try:
            df_clean = df.copy()

            numeric_cols = df_clean.select_dtypes(include=[np.number]).columns
            for col in numeric_cols:
                if df_clean[col].isna().any():
                    median_val = df_clean[col].median()
                    if pd.isna(median_val):
                        median_val = 0
                    df_clean[col] = df_clean[col].fillna(median_val)

            if len(df_clean) > 4:
                Q1 = df_clean['sales'].quantile(0.25)
                Q3 = df_clean['sales'].quantile(0.75)
                IQR = Q3 - Q1

                if IQR > 0:
                    lower_bound = Q1 - 1.5 * IQR
                    upper_bound = Q3 + 1.5 * IQR
                    df_clean['sales'] = df_clean['sales'].clip(lower_bound, upper_bound)

            df_clean = df_clean.replace([np.inf, -np.inf], 0)

            return df_clean

        except Exception as e:
            logger.error(f"Error cleaning data: {e}")
            return df.replace([np.inf, -np.inf], 0)

    def prepare_training_data(self, df: pd.DataFrame, test_size: float = 0.2):
        try:
            if len(df) < 2:
                logger.warning("Insufficient data for train/test split, using single point")
                exclude_cols = ['sales', 'orders', 'customers']
                feature_cols = [col for col in df.columns if col not in exclude_cols]

                X = df[feature_cols].replace([np.inf, -np.inf], np.nan).fillna(0)
                y = df['sales']

                return X, X, y, y

            exclude_cols = ['sales', 'orders', 'customers']
            feature_cols = [col for col in df.columns if col not in exclude_cols]

            X = df[feature_cols].replace([np.inf, -np.inf], np.nan).fillna(0)
            y = df['sales']

            split_idx = max(1, int(len(df) * (1 - test_size)))
            X_train, X_test = X.iloc[:split_idx], X.iloc[split_idx:]
            y_train, y_test = y.iloc[:split_idx], y.iloc[split_idx:]

            if len(X_test) == 0:
                X_test = X_train.iloc[-1:].copy()
                y_test = y_train.iloc[-1:].copy()

            return X_train, X_test, y_train, y_test

        except Exception as e:
            logger.error(f"Error preparing training data: {e}")
            raise

    def train_models(self, X_train, y_train, X_test, y_test) -> dict:
        try:
            if X_train is None or y_train is None or len(X_train) == 0:
                logger.warning("No training data available")
                return {}

            if getattr(y_train, "nunique", lambda: None)() is not None and y_train.nunique() <= 1:
                logger.warning("Target has no variance (constant). Skipping model training.")
                return {}

            models = {
                'Linear Regression': LinearRegression(),
                'Ridge Regression': Ridge(alpha=1.0),
                'Lasso Regression': Lasso(alpha=1.0)
            }

            results = {}

            self.scaler.fit(X_train)
            if hasattr(self.scaler, "scale_"):
                zero_scale_mask = (self.scaler.scale_ == 0)
                if zero_scale_mask.any():
                    self.scaler.scale_[zero_scale_mask] = 1.0

            X_train_scaled = self.scaler.transform(X_train)
            X_test_scaled = self.scaler.transform(X_test)

            for name, model in models.items():
                try:
                    n_features = X_train_scaled.shape[1]
                    n_samples = len(X_train_scaled)
                    if n_samples < 3 or y_train.nunique() <= 1:
                        selector = None
                        X_train_selected = X_train_scaled
                        X_test_selected = X_test_scaled
                    else:
                        k = min(10, n_features, max(1, n_samples - 1))
                        selector = SelectKBest(score_func=f_regression, k=k)
                        try:
                            X_train_selected = selector.fit_transform(X_train_scaled, y_train)
                            X_test_selected = selector.transform(X_test_scaled)
                        except Exception as sel_e:
                            logger.warning(f"Feature selection failed for {name}: {sel_e}. Using all features.")
                            selector = None
                            X_train_selected = X_train_scaled
                            X_test_selected = X_test_scaled

                    model.fit(X_train_selected, y_train)

                    train_pred = model.predict(X_train_selected)
                    test_pred = model.predict(X_test_selected)

                    train_mae = mean_absolute_error(y_train, train_pred) if len(y_train) > 0 else np.nan
                    test_mae = mean_absolute_error(y_test, test_pred) if len(y_test) > 0 else np.nan
                    train_rmse = np.sqrt(mean_squared_error(y_train, train_pred)) if len(y_train) > 0 else np.nan
                    test_rmse = np.sqrt(mean_squared_error(y_test, test_pred)) if len(y_test) > 0 else np.nan

                    results[name] = {
                        'model': model,
                        'selector': selector,
                        'train_mae': train_mae,
                        'test_mae': test_mae,
                        'train_rmse': train_rmse,
                        'test_rmse': test_rmse,
                        'predictions': test_pred
                    }

                except Exception as e:
                    logger.warning(f"Error training {name}: {e}", exc_info=True)
                    continue

            if not results:
                logger.error("No models could be trained successfully")
                return {}

            def score_key(k):
                v = results[k]
                return v['test_mae'] if not (v['test_mae'] is None or np.isnan(v['test_mae'])) else v['train_mae']

            best_model_name = min(results.keys(), key=score_key)
            self.model = results[best_model_name]['model']
            self.feature_selector = results[best_model_name]['selector']

            logger.info(f"Best model: {best_model_name}")
            return results

        except Exception as e:
            logger.error(f"Error training models: {e}", exc_info=True)
            return {}

    def generate_future_features(self, base_data: pd.DataFrame, forecast_days: int) -> pd.DataFrame:
        try:
            last_date = base_data.index.max() if len(base_data) > 0 else pd.Timestamp.now().normalize()
            future_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=forecast_days, freq='D')
            future_df = pd.DataFrame(index=future_dates)

            future_df['year'] = future_df.index.year
            future_df['month'] = future_df.index.month
            future_df['day_of_week'] = future_df.index.dayofweek
            future_df['day_of_year'] = future_df.index.dayofyear
            future_df['quarter'] = future_df.index.quarter
            future_df['is_weekend'] = future_df['day_of_week'].isin([5, 6]).astype(int)

            season_map = {12: 0, 1: 0, 2: 0, 3: 1, 4: 1, 5: 1, 6: 2, 7: 2, 8: 2, 9: 3, 10: 3, 11: 3}
            future_df['season'] = future_df['month'].map(season_map)

            try:
                holiday_dates = pd.to_datetime(self.get_holiday_dates())
                future_df['is_holiday'] = future_df.index.normalize().isin(holiday_dates.normalize()).astype(int)
            except Exception:
                future_df['is_holiday'] = 0

            if len(base_data) >= 1 and 'sales' in base_data.columns:
                last_sales = float(base_data['sales'].iloc[-1]) if not pd.isna(base_data['sales'].iloc[-1]) else float(
                    base_data['sales'].mean() or 0.0)
                recent_ma_7 = float(base_data['sales'].tail(7).mean()) if len(base_data) >= 1 else last_sales
                recent_ma_30 = float(base_data['sales'].tail(30).mean()) if len(base_data) >= 1 else last_sales

                if recent_ma_7 == 0 and (base_data['sales'] > 0).any():
                    recent_ma_7 = float(base_data['sales'][base_data['sales'] > 0].tail(7).mean() or last_sales or 1.0)
                if recent_ma_30 == 0 and (base_data['sales'] > 0).any():
                    recent_ma_30 = float(
                        base_data['sales'][base_data['sales'] > 0].tail(30).mean() or last_sales or 1.0)
            else:
                last_sales = recent_ma_7 = recent_ma_30 = 0.0

            future_df['sales_lag_1'] = last_sales
            future_df['sales_lag_7'] = last_sales
            future_df['sales_ma_7'] = recent_ma_7
            future_df['sales_ma_30'] = recent_ma_30
            future_df['sales_growth_7d'] = 0.0

            if len(base_data) > 0:
                future_df['trend'] = np.arange(len(base_data), len(base_data) + forecast_days)
            else:
                future_df['trend'] = np.arange(forecast_days)

            for col in base_data.columns:
                if col in ['sales', 'orders', 'customers']:
                    continue
                if col not in future_df.columns:
                    if np.issubdtype(base_data[col].dtype, np.number):
                        mean_val = base_data[col].mean()
                        future_df[col] = mean_val if not pd.isna(mean_val) else 0
                    else:
                        future_df[col] = base_data[col].mode().iloc[0] if len(base_data[col].mode()) > 0 else 0

            feature_cols = [col for col in base_data.columns if col not in ['sales', 'orders', 'customers']]
            future_df = future_df.reindex(columns=feature_cols, fill_value=0)

            return future_df

        except Exception as e:
            logger.error(f"Error generating future features: {e}", exc_info=True)
            last_date = base_data.index.max() if len(base_data) > 0 else pd.Timestamp.now()
            future_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=forecast_days, freq='D')
            future_df = pd.DataFrame(index=future_dates)
            future_df['trend'] = np.arange(forecast_days)
            future_df['month'] = future_df.index.month
            future_df['day_of_week'] = future_df.index.dayofweek
            return future_df

    def create_forecast(self, forecast_days: int = 30) -> dict:

        try:
            sales_data = self.get_sales_data()
            logger.info(f"Retrieved {len(sales_data)} days of sales data")

            if sales_data.empty:
                logger.warning("Sales data is empty after retrieval")

            logger.debug(f"Sales head:\n{sales_data.head() if not sales_data.empty else 'empty'}")

            features_data = self.create_features(sales_data)
            clean_data = self.clean_data(features_data)

            logger.info(f"Feature columns: {list(clean_data.columns)}")
            if 'sales' in clean_data.columns:
                logger.info(f"Clean sales range: {clean_data['sales'].min()} to {clean_data['sales'].max()}")
                logger.info(f"Non-zero sales days: {(clean_data['sales'] > 0).sum()}")

            X_train, X_test, y_train, y_test = self.prepare_training_data(clean_data)
            logger.info(f"Training data shape: X_train {X_train.shape}, y_train {y_train.shape}")
            try:
                logger.info(f"Training target range: {y_train.min()} to {y_train.max()}")
            except Exception:
                pass

            model_results = self.train_models(X_train, y_train, X_test, y_test)

            if not model_results:
                logger.warning("No models trained successfully, using fallback simple-average predictions")

                recent_avg = None
                if 'sales' in clean_data.columns and len(clean_data) > 0:
                    recent_avg = clean_data['sales'].tail(30).mean() if len(clean_data) >= 30 else clean_data[
                        'sales'].mean()
                if recent_avg is None or pd.isna(recent_avg) or recent_avg <= 0:
                    recent_avg = 500.0

                predictions = np.full(forecast_days, recent_avg, dtype=float)
                confidence_interval = recent_avg * 0.2

                self.results = {
                    'model_performance': {},
                    'predictions': predictions,
                    'confidence_lower': np.maximum(predictions - confidence_interval, 0.0),
                    'confidence_upper': predictions + confidence_interval,
                    'best_model': 'Simple Average',
                    'test_mae': None,
                    'test_rmse': None,
                    'historical_data': clean_data
                }
                logger.info(f"Fallback predictions used: {recent_avg}")
                return self.results

            def _score_key(k):
                v = model_results[k]
                tmae = v.get('test_mae', None)
                if tmae is None or (isinstance(tmae, float) and np.isnan(tmae)):
                    return v.get('train_mae', np.inf)
                return tmae

            best_model_name = min(model_results.keys(), key=_score_key)
            best_model_info = model_results[best_model_name]
            best_model = best_model_info['model']

            logger.info(f"Selected best model: {best_model_name} (test_mae={best_model_info.get('test_mae')})")

            future_features = self.generate_future_features(clean_data, forecast_days)
            logger.info(f"Generated {len(future_features)} future feature rows")
            logger.debug(f"Future features columns: {list(future_features.columns)}")

            exclude_cols = ['sales', 'orders', 'customers']
            feature_cols = [c for c in clean_data.columns if c not in exclude_cols]

            if len(feature_cols) == 0:
                logger.warning("No feature columns available after exclusion. Using simple-average fallback.")
                recent_avg = clean_data['sales'].tail(30).mean() if len(clean_data) >= 1 else 300.0
                predictions = np.full(forecast_days, recent_avg, dtype=float)
                confidence_interval = recent_avg * 0.2
                self.results = {
                    'model_performance': model_results,
                    'predictions': predictions,
                    'confidence_lower': np.maximum(predictions - confidence_interval, 0.0),
                    'confidence_upper': predictions + confidence_interval,
                    'best_model': 'Simple Average (no features)',
                    'test_mae': None,
                    'test_rmse': None,
                    'historical_data': clean_data
                }
                return self.results

            future_X = future_features.reindex(columns=feature_cols, fill_value=0)
            future_X = future_X.replace([np.inf, -np.inf], 0).fillna(0)

            try:
                if not hasattr(self.scaler, "scale_") or getattr(self.scaler, "scale_", None) is None:
                    try:
                        self.scaler.fit(X_train)
                    except Exception as e:
                        logger.warning(f"Scaler fit on X_train failed: {e}. Attempting to fit on future_X.")
                        try:
                            self.scaler.fit(future_X)
                        except Exception as e2:
                            logger.warning(f"Scaler fallback fit failed: {e2}. Using identity scaling.")
                            self.scaler = None
                if self.scaler is not None and hasattr(self.scaler, "scale_"):
                    zero_mask = (self.scaler.scale_ == 0)
                    if zero_mask.any():
                        self.scaler.scale_[zero_mask] = 1.0
            except Exception as e:
                logger.warning(f"Scaler safety checks failed: {e}")
                self.scaler = None

            try:
                if self.scaler is not None:
                    future_X_scaled = self.scaler.transform(future_X)
                else:
                    future_X_scaled = future_X.values
            except Exception as e:
                logger.warning(f"Scaling future features failed: {e}. Using unscaled features.")
                future_X_scaled = future_X.values

            selector = best_model_info.get('selector', None)
            if selector is not None:
                try:
                    future_X_selected = selector.transform(future_X_scaled)
                except Exception as e:
                    logger.warning(f"Applying selector.transform failed: {e}. Using full feature set instead.")
                    future_X_selected = future_X_scaled
            else:
                future_X_selected = future_X_scaled

            try:
                raw_predictions = best_model.predict(future_X_selected)
                raw_predictions = np.nan_to_num(raw_predictions, nan=0.0,
                                                posinf=np.max(raw_predictions) if raw_predictions.size else 0.0,
                                                neginf=0.0)
            except Exception as e:
                logger.warning(f"Model prediction failed: {e}. Falling back to recent average.")
                raw_predictions = np.zeros(forecast_days, dtype=float)

            if raw_predictions.size == 0 or np.all(raw_predictions <= 0):
                logger.warning("Raw predictions are all <= 0 or empty; using recent-average fallback.")
                recent_avg = None
                if 'sales' in clean_data.columns and len(clean_data) > 0:
                    recent_avg = clean_data['sales'].tail(30).mean() if len(clean_data) >= 30 else clean_data[
                        'sales'].mean()
                if recent_avg is None or pd.isna(recent_avg) or recent_avg <= 0:
                    recent_avg = max(1.0, clean_data['sales'].mean() if len(clean_data) else 300.0)
                predictions = np.full(forecast_days, recent_avg, dtype=float)
            else:
                predictions = np.maximum(raw_predictions, 1.0)

            logger.info(
                f"Predictions summary: min={predictions.min()}, max={predictions.max()}, mean={predictions.mean()}")

            test_preds = best_model_info.get('predictions', None)
            test_residuals = None
            try:
                if test_preds is not None and len(y_test) == len(test_preds) and len(y_test) > 0:
                    test_residuals = y_test - np.array(test_preds)
                else:
                    if X_test is not None and len(X_test) > 0:
                        if self.scaler is not None:
                            X_test_scaled = self.scaler.transform(X_test.reindex(columns=feature_cols, fill_value=0))
                        else:
                            X_test_scaled = X_test.reindex(columns=feature_cols, fill_value=0).values
                        if selector is not None:
                            try:
                                X_test_selected = selector.transform(X_test_scaled)
                            except Exception:
                                X_test_selected = X_test_scaled
                        else:
                            X_test_selected = X_test_scaled
                        try:
                            preds_on_test = best_model.predict(X_test_selected)
                            test_residuals = y_test - preds_on_test
                        except Exception:
                            test_residuals = None

                if test_residuals is None:
                    if y_train is not None and len(y_train) > 1:
                        try:
                            if self.scaler is not None:
                                X_train_scaled = self.scaler.transform(
                                    X_train.reindex(columns=feature_cols, fill_value=0))
                            else:
                                X_train_scaled = X_train.reindex(columns=feature_cols, fill_value=0).values
                            if selector is not None:
                                try:
                                    X_train_selected = selector.transform(X_train_scaled)
                                except Exception:
                                    X_train_selected = X_train_scaled
                            else:
                                X_train_selected = X_train_scaled
                            train_preds = best_model.predict(X_train_selected)
                            test_residuals = y_train - train_preds
                        except Exception:
                            test_residuals = None

                if test_residuals is None:
                    test_residuals = predictions * 0.2

                test_residuals = np.asarray(test_residuals, dtype=float)
            except Exception as e:
                logger.warning(f"Failed computing residuals: {e}. Using heuristic residuals.")
                test_residuals = predictions * 0.2

            try:
                std_error = np.std(test_residuals) if len(test_residuals) > 1 else (np.mean(predictions) * 0.15)
                if pd.isna(std_error) or std_error == 0:
                    std_error = np.mean(predictions) * 0.15
            except Exception:
                std_error = np.mean(predictions) * 0.15

            confidence_interval = 1.96 * std_error

            self.results = {
                'model_performance': model_results,
                'predictions': predictions,
                'confidence_lower': np.maximum(predictions - confidence_interval, 0.0),
                'confidence_upper': predictions + confidence_interval,
                'best_model': best_model_name,
                'test_mae': best_model_info.get('test_mae', None),
                'test_rmse': best_model_info.get('test_rmse', None),
                'historical_data': clean_data
            }

            logger.info(
                f"Final predictions range: {self.results['predictions'].min()} to {self.results['predictions'].max()}")
            logger.info(f"Forecast created with {len(self.results['predictions'])} predictions")
            return self.results

        except Exception as e:
            logger.error(f"Forecasting failed: {e}", exc_info=True)
            fallback_value = 300.0
            self.results = {
                'model_performance': {},
                'predictions': np.full(forecast_days, fallback_value, dtype=float),
                'confidence_lower': np.full(forecast_days, fallback_value * 0.8, dtype=float),
                'confidence_upper': np.full(forecast_days, fallback_value * 1.2, dtype=float),
                'best_model': 'Fallback',
                'test_mae': None,
                'test_rmse': None,
                'error': str(e),
                'historical_data': pd.DataFrame()
            }
            return self.results

        except Exception as e:
            logger.error(f"Forecasting failed: {e}", exc_info=True)
            fallback_value = 300.0
            self.results = {
                'model_performance': {},
                'predictions': np.full(forecast_days, fallback_value),
                'confidence_lower': np.full(forecast_days, fallback_value * 0.8),
                'confidence_upper': np.full(forecast_days, fallback_value * 1.2),
                'best_model': 'Fallback',
                'test_mae': 0.0,
                'test_rmse': 0.0,
                'error': str(e),
                'historical_data': pd.DataFrame()
            }
            return self.results