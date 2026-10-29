import { useEffect, useState } from 'react';

export function useListState() {
  const [search, setSearch] = useState('');
  const [committed, setCommitted] = useState('');
  const [page, setPage] = useState(1);
  const [limit, updateLimit] = useState(20);
  useEffect(() => {
    const timer = setTimeout(() => {
      setCommitted(search.trim());
      setPage(1);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);
  return {
    search,
    setSearch,
    committed,
    page,
    setPage,
    limit,
    setLimit: (n: number) => {
      updateLimit(n);
      setPage(1);
    },
    filters: { search: committed, page, limit },
  };
}
