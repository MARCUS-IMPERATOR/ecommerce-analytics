import React, { useState, useEffect } from 'react';
import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import apiClient from '../../services/api';

interface TopCustomerData {
  customerId: number;
  customerName: string;
  totalSpent: number;
  orderCount: number;
  segment: string;
  ltv: number;
}

interface CustomerAnalytics {
  topCustomers: TopCustomerData[];
}


interface TableProps {
  startDate: string;
  endDate: string;  
  thresholdDate:string
}

type SortField = 'customerName' | 'totalSpent' | 'orderCount' | 'ltv';
type SortDirection = 'asc' | 'desc';

const TopCustomersTable: React.FC<TableProps> = (
    {
        startDate,
        endDate,  
        thresholdDate
    }
) => {
  const [customers, setCustomers] = useState<TopCustomerData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>('ltv');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const fetchCustomerAnalytics = async () => {
    try {
      setLoading(true);      
      const data: CustomerAnalytics = await apiClient.get(
        `analytics/customerAnalytics?start=${startDate}T00:00:00&end=${endDate}T23:59:59&threshold=${thresholdDate}T00:00:00`
      );
      if (!data || !data.topCustomers?.length) {
        setCustomers([])
        return;
        }

      setCustomers(data.topCustomers);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
      console.log("ERROR")
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    fetchCustomerAnalytics();
  }, [startDate,endDate,thresholdDate]);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const sortedCustomers = [...customers].sort((a, b) => {
    const aValue = a[sortField];
    const bValue = b[sortField];
    
    let comparison = 0;
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      comparison = aValue.localeCompare(bValue);
    } else if (typeof aValue === 'number' && typeof bValue === 'number') {
      comparison = aValue - bValue;
    }
    
    return sortDirection === 'asc' ? comparison : -comparison;
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const getSegmentStyle = (segment: string) => {
    switch (segment) {
      case 'CHAMPION':
        return { backgroundColor: '#dcfce7', color: '#166534', border: '1px solid #bbf7d0' };
      case 'LOYAL':
        return { backgroundColor: '#dbeafe', color: '#1e40af', border: '1px solid #bfdbfe' };
      case 'NEW':
        return { backgroundColor: '#fed7aa', color: '#c2410c', border: '1px solid #fdba74' };
      default:
        return { backgroundColor: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db' };
    }
  };

  const SortIcon: React.FC<{ field: SortField }> = ({ field }) => {
    if (sortField !== field) {
      return <ArrowUpDown style={{ width: '16px', height: '16px', color: '#9ca3af' }} />;
    }
    return sortDirection === 'asc' 
      ? <ArrowUp style={{ width: '16px', height: '16px', color: '#2563eb' }} />
      : <ArrowDown style={{ width: '16px', height: '16px', color: '#2563eb' }} />;
  };

  const styles = {
    container: {
      width: '100%',
      maxWidth: '1000px',
      margin: '0 auto',
      padding: '0',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif'
    },
    card: {
      backgroundColor: 'white',
      borderRadius: '8px',
      boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
      overflow: 'hidden'
    },
    header: {
      padding: '24px',
      borderBottom: '1px solid #e5e7eb',
      backgroundColor: '#f9fafb'
    },
    title: {
      fontSize: '24px',
      fontWeight: 'bold',
      color: '#111827',
      margin: '0 0 4px 0'
    },
    subtitle: {
      color: '#6b7280',
      margin: 0,
      fontSize: '14px'
    },
    tableContainer: {
      maxHeight: '800px',  // Set a fixed height for the table container (adjust as necessary)
      overflowY: 'auto'  // Enables vertical scrolling
    },
    table: {
      width: '100%',
      borderCollapse: 'collapse' as const
    },
    thead: {
      backgroundColor: '#f9fafb'
    },
    th: {
      padding: '12px 24px',
      textAlign: 'left' as const,
      fontSize: '11px',
      fontWeight: '500',
      color: '#6b7280',
      textTransform: 'uppercase' as const,
      letterSpacing: '0.05em',
      borderBottom: '1px solid #e5e7eb'
    },
    sortButton: {
      display: 'flex',
      alignItems: 'center',
      gap: '4px',
      background: 'none',
      border: 'none',
      cursor: 'pointer',
      color: 'inherit',
      fontSize: 'inherit',
      fontWeight: 'inherit',
      textTransform: 'inherit' as const,
      letterSpacing: 'inherit',
      padding: 0
    },
    tbody: {
      backgroundColor: 'white'
    },
    tr: {
      borderBottom: '1px solid #e5e7eb'
    },
    trHover: {
      backgroundColor: '#f9fafb'
    },
    td: {
      padding: '16px 24px',
      whiteSpace: 'nowrap' as const
    },
    customerCell: {
      display: 'flex',
      alignItems: 'center'
    },
    avatar: {
      height: '40px',
      width: '40px',
      borderRadius: '50%',
      backgroundColor: '#3b82f6',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0
    },
    avatarText: {
      fontSize: '14px',
      fontWeight: '500',
      color: 'white'
    },
    customerInfo: {
      marginLeft: '16px'
    },
    customerName: {
      fontSize: '14px',
      fontWeight: '500',
      color: '#111827',
      margin: '0 0 2px 0'
    },
    customerId: {
      fontSize: '12px',
      color: '#6b7280',
      margin: 0
    },
    segmentBadge: {
      display: 'inline-flex',
      padding: '4px 8px',
      fontSize: '12px',
      fontWeight: '600',
      borderRadius: '9999px'
    },
    currency: {
      fontSize: '14px',
      fontWeight: '500',
      color: '#111827'
    },
    orderCount: {
      fontSize: '14px',
      color: '#111827'
    },
    ltv: {
      fontSize: '14px',
      fontWeight: 'bold',
      color: '#059669'
    },
    topBadge: {
      display: 'inline-flex',
      alignItems: 'center',
      padding: '4px 8px',
      borderRadius: '9999px',
      fontSize: '12px',
      fontWeight: '500',
      backgroundColor: '#fef3c7',
      color: '#d97706',
      marginLeft: '8px'
    },
    loading: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '32px',
      gap: '8px'
    },
    spinner: {
      width: '32px',
      height: '32px',
      border: '3px solid #e5e7eb',
      borderTop: '3px solid #3b82f6',
      borderRadius: '50%',
      animation: 'spin 1s linear infinite'
    },
    error: {
      backgroundColor: '#fef2f2',
      border: '1px solid #fecaca',
      borderRadius: '8px',
      padding: '16px',
      color: '#dc2626'
    },
    errorTitle: {
      fontWeight: '600',
      marginBottom: '8px'
    },
    retryButton: {
      marginTop: '12px',
      padding: '8px 16px',
      backgroundColor: '#dc2626',
      color: 'white',
      border: 'none',
      borderRadius: '4px',
      cursor: 'pointer',
      fontSize: '14px'
    },
    emptyState: {
      textAlign: 'center' as const,
      padding: '32px',
      color: '#6b7280'
    }
  };

  if (loading) {
    return (
      <div style={styles.loading}>
        <div style={styles.spinner}></div>
        <span style={{ color: '#6b7280' }}>Loading customer data...</span>
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
        <h3 style={styles.errorTitle}>Error loading data</h3>
        <p>{error}</p>
        <button
          onClick={fetchCustomerAnalytics}
          style={styles.retryButton}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#b91c1c'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#dc2626'}
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.header}>
          <h2 style={styles.title}>Top Customers by Lifetime Value</h2>
          <p style={styles.subtitle}>Showing {customers.length} customers</p>
        </div>
        
        <div style={styles.tableContainer}>
          <table style={styles.table}>
            <thead style={styles.thead}>
              <tr>
                <th style={styles.th}>
                  <button
                    onClick={() => handleSort('customerName')}
                    style={styles.sortButton}
                  >
                    <span>Customer Name</span>
                    <SortIcon field="customerName" />
                  </button>
                </th>
                <th style={styles.th}>Segment</th>
                <th style={styles.th}>
                  <button
                    onClick={() => handleSort('totalSpent')}
                    style={styles.sortButton}
                  >
                    <span>Total Spent</span>
                    <SortIcon field="totalSpent" />
                  </button>
                </th>
                <th style={styles.th}>
                  <button
                    onClick={() => handleSort('orderCount')}
                    style={styles.sortButton}
                  >
                    <span>Orders</span>
                    <SortIcon field="orderCount" />
                  </button>
                </th>
                <th style={styles.th}>
                  <button
                    onClick={() => handleSort('ltv')}
                    style={styles.sortButton}
                  >
                    <span>Lifetime Value</span>
                    <SortIcon field="ltv" />
                  </button>
                </th>
              </tr>
            </thead>
            <tbody style={styles.tbody}>
              {sortedCustomers.map((customer, index) => (
                <tr 
                  key={customer.customerId} 
                  style={styles.tr}
                  onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f9fafb'}
                  onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'white'}
                >
                  <td style={styles.td}>
                    <div style={styles.customerCell}>
                      <div style={styles.avatar}>
                        <span style={styles.avatarText}>
                          {customer.customerName.split(' ').map(n => n[0]).join('')}
                        </span>
                      </div>
                      <div style={styles.customerInfo}>
                        <div style={styles.customerName}>
                          {customer.customerName}
                        </div>
                        <div style={styles.customerId}>
                          ID: {customer.customerId}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td style={styles.td}>
                    <span style={{...styles.segmentBadge, ...getSegmentStyle(customer.segment)}}>
                      {customer.segment}
                    </span>
                  </td>
                  <td style={styles.td}>
                    <div style={styles.currency}>
                      {formatCurrency(customer.totalSpent)}
                    </div>
                  </td>
                  <td style={styles.td}>
                    <div style={styles.orderCount}>
                      {customer.orderCount.toLocaleString()}
                    </div>
                  </td>
                  <td style={styles.td}>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                      <span style={styles.ltv}>
                        {formatCurrency(customer.ltv)}
                      </span>
                      {index === 0 && (
                        <span style={styles.topBadge}>
                          Top LTV
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        {customers.length === 0 && (
          <div style={styles.emptyState}>
            No customer data available
          </div>
        )}
      </div>
    </div>
  );
};

export default TopCustomersTable;