const BASE_URL = '/api/v1';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`API error ${res.status}: ${err}`);
  }
  return res.json();
}

export const api = {
  dashboard: {
    stats: () => request<import('../types').DashboardStats>('/admin/dashboard'),
  },
  participants: {
    list: () => request<import('../types').Participant[]>('/admin/participants'),
    get: (id: string) => request<import('../types').Participant>(`/admin/participants/${id}`),
    create: (data: Partial<import('../types').Participant>) =>
      request<import('../types').Participant>('/admin/participants', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: string, data: Partial<import('../types').Participant>) =>
      request<import('../types').Participant>(`/admin/participants/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => request<void>(`/admin/participants/${id}`, { method: 'DELETE' }),
  },
  routingRules: {
    list: () => request<import('../types').RoutingRule[]>('/admin/routing-rules'),
    create: (data: Partial<import('../types').RoutingRule>) =>
      request<import('../types').RoutingRule>('/admin/routing-rules', { method: 'POST', body: JSON.stringify(data) }),
  },
  transactions: {
    list: (page = 0, size = 20) =>
      request<{ content: import('../types').Transaction[] }>(`/switch/transactions?page=${page}&size=${size}`),
    get: (id: string) => request<import('../types').Transaction>(`/switch/transactions/${id}`),
  },
  binTables: {
    list: () => request<import('../types').BinTable[]>('/admin/bin-tables'),
  },
};
