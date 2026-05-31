interface Column<T> {
  key: string;
  title: string;
  width?: string;
  render?: (value: any, record: T, index: number) => ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  rowKey?: string;
  onRowClick?: (record: T) => void;
  size?: "default" | "compact";
  pagination?: {
    page: number;
    pageSize: number;
    total: number;
    onChange: (page: number, pageSize: number) => void;
  } | false;
}

export default function DataTable<T extends Record<string, any>>({
  columns,
  data,
  rowKey = 'id',
  onRowClick,
  size = "compact",
  pagination = false,
}: DataTableProps<T>) {
  const padding = size === "compact" ? "px-2 py-1.5" : "px-3 py-2";

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={`${padding} text-left font-semibold text-gray-700`}
                  style={{ width: col.width }}
                >
                  {col.title}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-100">
            {data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="py-8 text-center text-gray-400">
                  暂无数据
                </td>
              </tr>
            ) : (
              data.map((record, index) => (
                <tr
                  key={record[rowKey] || index}
                  onClick={() => onRowClick?.(record)}
                  className={`${onRowClick ? 'cursor-pointer hover:bg-gray-50' : ''}`}
                >
                  {columns.map((col) => (
                    <td key={col.key} className={`${padding} text-gray-600`}>
                      {col.render
                        ? col.render(record[col.key], record, index)
                        : record[col.key]
                      }
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {pagination && (
        <div className="flex items-center justify-between px-3 py-2 border-t bg-gray-50">
          <div className="text-xs text-gray-500">
            共 {pagination.total} 条
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => pagination.onChange(pagination.page - 1, pagination.pageSize)}
              disabled={pagination.page <= 1}
              className="px-2 py-1 text-xs border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              上一页
            </button>
            <span className="text-xs">
              第 {pagination.page} / {Math.ceil(pagination.total / pagination.pageSize)} 页
            </span>
            <button
              onClick={() => pagination.onChange(pagination.page + 1, pagination.pageSize)}
              disabled={pagination.page >= Math.ceil(pagination.total / pagination.pageSize)}
              className="px-2 py-1 text-xs border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              下一页
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
