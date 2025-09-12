import React, { useEffect, useState } from "react";
import { ArrowUpDown, ArrowUp, ArrowDown, AlertTriangle } from "lucide-react";
import apiClient from "../../services/api";

interface LowStockProduct {
  createdAt?: string;
  updatedAt?: string;
  productId: number;
  sku?: string;
  name: string;
  description?: string;
  category?: string;
  brand?: string;
  price?: number;
  stockQuantity: number;
}

interface Props {
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  lowStockthreshold?: number; // stock threshold (e.g. 10)
}

type SortField = "name" | "sku" | "category" | "brand" | "price" | "stockQuantity";
type SortDirection = "asc" | "desc";

const LowStockTable: React.FC<Props> = ({ startDate, endDate, lowStockthreshold = 10 }) => {
  const [products, setProducts] = useState<LowStockProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>("stockQuantity");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");

  const fetchLowStock = async () => {
    try {
      setLoading(true);
      setError(null);
      // NOTE: your apiClient in examples returns the JSON body directly (no .data), keep consistent
      const data = await apiClient.get(
        `analytics/productsPerformance?start=${startDate}T00:00:00&end=${endDate}T23:59:59&threshold=${lowStockthreshold}`
      );

      const list: LowStockProduct[] = data?.lowStockAlerts ?? [];
      setProducts(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load low stock data");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLowStock();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startDate, endDate, lowStockthreshold]);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDirection("asc");
    }
  };

  const sorted = [...products].sort((a, b) => {
    const aVal = a[sortField];
    const bVal = b[sortField];
    let comp = 0;
    if (typeof aVal === "string" && typeof bVal === "string") comp = aVal.localeCompare(bVal);
    else if (typeof aVal === "number" && typeof bVal === "number") comp = aVal - bVal;
    return sortDirection === "asc" ? comp : -comp;
  });

  const formatCurrency = (v?: number) =>
    v == null ? "-" : new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 }).format(v);

  const SortIcon: React.FC<{ field: SortField }> = ({ field }) => {
    if (sortField !== field) return <ArrowUpDown style={{ width: 16, height: 16, color: "#9ca3af" }} />;
    return sortDirection === "asc" ? (
      <ArrowUp style={{ width: 16, height: 16, color: "#2563eb" }} />
    ) : (
      <ArrowDown style={{ width: 16, height: 16, color: "#2563eb" }} />
    );
  };

  const styles: Record<string, React.CSSProperties> = {
    card: {
      background: "white",
      borderRadius: 8,
      boxShadow: "0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05)",
      overflow: "hidden",
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    },
    header: { padding: 20, borderBottom: "1px solid #e5e7eb", background: "#f8fafc" },
    title: { margin: 0, fontSize: 18, fontWeight: 700, color: "#111827" },
    subtitle: { margin: 0, fontSize: 13, color: "#6b7280" },
    tableContainer: { maxHeight: 420, overflowY: "auto" },
    table: { width: "100%", borderCollapse: "collapse" as const },
    th: { padding: "12px 16px", textAlign: "left" as const, fontSize: 12, color: "#6b7280", borderBottom: "1px solid #e5e7eb" },
    td: { padding: "12px 16px", borderBottom: "1px solid #f3f4f6", whiteSpace: "nowrap" as const },
    sku: { fontSize: 12, color: "#6b7280" },
    productName: { fontSize: 14, fontWeight: 600, color: "#111827" },
    badgeLow: { display: "inline-block", padding: "4px 8px", borderRadius: 999, background: "#fff7ed", color: "#c2410c", fontWeight: 600, fontSize: 12 },
    badgeOut: { display: "inline-block", padding: "4px 8px", borderRadius: 999, background: "#fee2e2", color: "#991b1b", fontWeight: 700, fontSize: 12 },
    spinner: { width: 32, height: 32, border: "3px solid #e5e7eb", borderTop: "3px solid #3b82f6", borderRadius: "50%", animation: "spin 1s linear infinite" },
    footer: { padding: 12, borderTop: "1px solid #e5e7eb", background: "#fff" },
  };

  if (loading) {
    return (
      <div style={styles.card}>
        <div style={styles.header}>
          <h3 style={styles.title}>Low Stock Products</h3>
          <p style={styles.subtitle}>Checking products below stock threshold of {lowStockthreshold}</p>
        </div>
        <div style={{ padding: 24, display: "flex", alignItems: "center", gap: 12 }}>
          <div style={styles.spinner}></div>
          <div style={{ color: "#6b7280" }}>Loading low stock products...</div>
        </div>
        <style>{`@keyframes spin { from { transform: rotate(0deg);} to { transform: rotate(360deg);} }`}</style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.card}>
        <div style={styles.header}>
          <h3 style={styles.title}>Low Stock Products</h3>
          <p style={styles.subtitle}>Error</p>
        </div>
        <div style={{ padding: 16 }}>
          <div style={{ background: "#fff7f7", padding: 12, borderRadius: 8, color: "#991b1b" }}>{error}</div>
          <div style={{ marginTop: 12 }}>
            <button onClick={fetchLowStock} style={{ padding: "8px 12px", background: "#dc2626", color: "white", border: "none", borderRadius: 6, cursor: "pointer" }}>
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.card}>
      <div style={styles.header}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <h3 style={styles.title}>Low Stock Products</h3>
            <p style={styles.subtitle}>Products with stock ≤ {lowStockthreshold} — {products.length} items</p>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <AlertTriangle style={{ width: 20, height: 20, color: "#f59e0b" }} />
            <button onClick={fetchLowStock} style={{ padding: "8px 10px", borderRadius: 6, border: "1px solid #e5e7eb", background: "white", cursor: "pointer" }}>
              Refresh
            </button>
          </div>
        </div>
      </div>

      <div style={styles.tableContainer}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>
                <button onClick={() => handleSort("name")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  Name <SortIcon field="name" />
                </button>
              </th>
              <th style={styles.th}>
                <button onClick={() => handleSort("sku")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  SKU <SortIcon field="sku" />
                </button>
              </th>
              <th style={styles.th}>
                <button onClick={() => handleSort("category")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  Category <SortIcon field="category" />
                </button>
              </th>
              <th style={styles.th}>
                <button onClick={() => handleSort("brand")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  Brand <SortIcon field="brand" />
                </button>
              </th>
              <th style={styles.th}>
                <button onClick={() => handleSort("price")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  Price <SortIcon field="price" />
                </button>
              </th>
              <th style={styles.th}>
                <button onClick={() => handleSort("stockQuantity")} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", gap: 8, alignItems: "center" }}>
                  Stock <SortIcon field="stockQuantity" />
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((p) => (
              <tr key={p.productId} style={{ background: p.stockQuantity === 0 ? "#fff7f7" : "white" }}>
                <td style={styles.td}>
                  <div style={{ display: "flex", flexDirection: "column" }}>
                    <span style={styles.productName}>{p.name}</span>
                    <span style={{ fontSize: 12, color: "#6b7280" }}>{p.description ?? ""}</span>
                  </div>
                </td>
                <td style={{ ...styles.td, ...styles.sku }}>{p.sku ?? "-"}</td>
                <td style={styles.td}>{p.category ?? "-"}</td>
                <td style={styles.td}>{p.brand ?? "-"}</td>
                <td style={styles.td}>{formatCurrency(p.price)}</td>
                <td style={styles.td}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <div style={p.stockQuantity === 0 ? styles.badgeOut : styles.badgeLow}>{p.stockQuantity}</div>
                    {p.stockQuantity === 0 ? <span style={{ color: "#374151", fontSize: 12 }}>Out of stock</span> : <span style={{ color: "#6b7280", fontSize: 12 }}>Low</span>}
                  </div>
                </td>
              </tr>
            ))}
            {sorted.length === 0 && (
              <tr>
                <td colSpan={6} style={{ padding: 24, textAlign: "center", color: "#6b7280" }}>
                  No low-stock products found for the selected range/threshold.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={styles.footer}>
        <small style={{ color: "#6b7280" }}>Tip: increase the threshold prop to show more items (e.g. threshold={20}).</small>
      </div>

      <style>{`@keyframes spin { from { transform: rotate(0deg);} to { transform: rotate(360deg);} }`}</style>
    </div>
  );
};

export default LowStockTable;