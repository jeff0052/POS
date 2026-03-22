import { useEffect, useState } from "react";

export function useAsyncData<T>(loader: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    setLoading(true);
    loader()
      .then((result) => {
        if (!active) return;
        setData(result);
        setError(null);
      })
      .catch((err: Error) => {
        if (!active) return;
        setError(err.message || "Unknown error");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [loader]);

  return { data, loading, error };
}
