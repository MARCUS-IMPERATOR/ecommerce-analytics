import React, { useEffect, useState } from 'react';
import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import apiClient from '../../services/api';

interface ProductRecommendation {
  productId: number;
  productName: string;
  recommendationCount: number;
  averageScore: number;
}

interface RecommendationAnalytics {
  topRecommendedProducts: ProductRecommendation[];
  totalCustomers: number;
  averageRecommendations: number;
}

type SortField = 'productName' | 'recommendationCount' | 'averageScore';
type SortDirection = 'asc' | 'desc';

const TopRecommendedProductsTable: React.FC = () => {
  const [products, setProducts] = useState<ProductRecommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>('recommendationCount');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const fetchRecommendations = async () => {
    try {
      setLoading(true);
      const data: RecommendationAnalytics = await apiClient.get('/analytics/recommendations');
      setProducts(data.topRecommendedProducts || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendations();
  }, []);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const sortedProducts = [...products].sort((a, b) => {
    const aVal = a[sortField];
    const bVal = b[sortField];

    let comp = 0;
    if (typeof aVal === 'string') {
      comp = aVal.localeCompare(bVal as string);
    } else {
      comp = (aVal as number) - (bVal as number);
    }

    return sortDirection === 'asc' ? comp : -comp;
  });

  const SortIcon: React.FC<{ field: SortField }> = ({ field }) => {
    if (sortField !== field) {
      return <ArrowUpDown style={{ width: '16px', height: '16px', color: '#9ca3af' }} />;
    }
    return sortDirection === 'asc'
      ? <ArrowUp style={{ width: '16px', height: '16px', color: '#2563eb' }} />
      : <ArrowDown style={{ width: '16px', height: '16px', color: '#2563eb' }} />;
  };

  const getAvatarText = (name: string) => {
    return name
      .split(' ')
      .map(word => word[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  };

  const styles = {
    container: {
      width: '100%',
      maxWidth: '1000px',
      margin: '0 auto',
      fontFamily: 'system-ui, sans-serif',
    },
    card: {
      backgroundColor: 'white',
      borderRadius: '8px',
      boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
      overflow: 'hidden',
    },
    header: {
      padding: '24px',
      borderBottom: '1px solid #e5e7eb',
      backgroundColor: '#f9fafb',
    },
    title: {
      fontSize: '24px',
      fontWeight: 'bold',
      color: '#111827',
      margin: '0 0 4px 0',
    },
    subtitle: {
      color: '#6b7280',
      fontSize: '14px',
    },
    tableContainer: {
      maxHeight: '800px',
      overflowY: 'auto',
    },
    table: {
      width: '100%',
      borderCollapse: 'collapse' as const,
    },
    th: {
      padding: '12px 24px',
      textAlign: 'left' as const,
      fontSize: '11px',
      fontWeight: 500,
      color: '#6b7280',
      textTransform: 'uppercase' as const,
      letterSpacing: '0.05em',
      borderBottom: '1px solid #e5e7eb',
    },
    sortButton: {
      display: 'flex',
      alignItems: 'center',
      gap: '4px',
      background: 'none',
      border: 'none',
      cursor: 'pointer',
      fontSize: 'inherit',
      padding: 0,
    },
    td: {
      padding: '16px 24px',
      whiteSpace: 'nowrap' as const,
    },
    tr: {
      borderBottom: '1px solid #e5e7eb',
    },
    avatar: {
      height: '40px',
      width: '40px',
      borderRadius: '50%',
      backgroundColor: '#3b82f6',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
    },
    avatarText: {
      fontSize: '14px',
      fontWeight: '500',
      color: 'white',
    },
    productInfo: {
      marginLeft: '16px',
    },
    productName: {
      fontSize: '14px',
      fontWeight: '500',
      color: '#111827',
      margin: 0,
    },
    productId: {
      fontSize: '12px',
      color: '#6b7280',
    },
    topBadge: {
      display: 'inline-flex',
      padding: '4px 8px',
      borderRadius: '9999px',
      backgroundColor: '#fef3c7',
      color: '#d97706',
      fontWeight: 500,
      fontSize: '12px',
      marginLeft: '8px',
    },
    loading: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '32px',
      gap: '8px',
    },
    spinner: {
      width: '32px',
      height: '32px',
      border: '3px solid #e5e7eb',
      borderTop: '3px solid #3b82f6',
      borderRadius: '50%',
      animation: 'spin 1s linear infinite',
    },
    error: {
      backgroundColor: '#fef2f2',
      border: '1px solid #fecaca',
      borderRadius: '8px',
      padding: '16px',
      color: '#dc2626',
    },
    emptyState: {
      textAlign: 'center' as const,
      padding: '32px',
      color: '#6b7280',
    },
  };

  if (loading) {
    return (
      <div style={styles.loading}>
        <div style={styles.spinner}></div>
        <span style={{ color: '#6b7280' }}>Loading recommendation data...</span>
        <style>
          {`
            @keyframes spin {
              from { transform: rotate(0deg); }
              to { transform: rotate(360deg); }
            }
          `}
        </style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.error}>
        <h3>Error loading data</h3>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.header}>
          <h2 style={styles.title}>Top Recommended Products</h2>
          <p style={styles.subtitle}>Showing {products.length} products</p>
        </div>

        <div style={styles.tableContainer}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>
                  <button onClick={() => handleSort('productName')} style={styles.sortButton}>
                    Product Name
                    <SortIcon field="productName" />
                  </button>
                </th>
                <th style={styles.th}>
                  <button onClick={() => handleSort('recommendationCount')} style={styles.sortButton}>
                    Recommendation Count
                    <SortIcon field="recommendationCount" />
                  </button>
                </th>
                <th style={styles.th}>
                  <button onClick={() => handleSort('averageScore')} style={styles.sortButton}>
                    Average Score
                    <SortIcon field="averageScore" />
                  </button>
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedProducts.map((product, index) => (
                <tr key={product.productId} style={styles.tr}>
                  <td style={{ ...styles.td, display: 'flex', alignItems: 'center' }}>
                    <div style={styles.avatar}>
                      <span style={styles.avatarText}>{getAvatarText(product.productName)}</span>
                    </div>
                    <div style={styles.productInfo}>
                      <p style={styles.productName}>
                        {product.productName}
                        {index === 0 && <span style={styles.topBadge}>Top</span>}
                      </p>
                      <p style={styles.productId}>ID: {product.productId}</p>
                    </div>
                  </td>
                  <td style={styles.td}>{product.recommendationCount}</td>
                  <td style={styles.td}>{product.averageScore.toFixed(3)}</td>
                </tr>
              ))}
              {sortedProducts.length === 0 && (
                <tr>
                  <td colSpan={3} style={styles.emptyState}>
                    No recommendations found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default TopRecommendedProductsTable;