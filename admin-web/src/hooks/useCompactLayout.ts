import { useEffect, useState } from 'react';

export function useCompactLayout() {
  const [compact, setCompact] = useState(
    () => window.matchMedia?.('(max-width: 1000px)').matches ?? false,
  );
  useEffect(() => {
    if (!window.matchMedia) return;
    const media = window.matchMedia('(max-width: 1000px)');
    const update = () => setCompact(media.matches);
    update();
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);
  return compact;
}
