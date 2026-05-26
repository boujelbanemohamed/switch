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
  issuing: {
    cardholders: {
      list: () => request<import('../types').Cardholder[]>('/issuing/cardholders'),
      get: (id: string) => request<import('../types').Cardholder>(`/issuing/cardholders/${id}`),
      create: (data: Partial<import('../types').Cardholder>) =>
        request<import('../types').Cardholder>('/issuing/cardholders', { method: 'POST', body: JSON.stringify(data) }),
      block: (id: string) =>
        request<import('../types').Cardholder>(`/issuing/cardholders/${id}/block`, { method: 'POST' }),
    },
    cards: {
      get: (id: string) => request<import('../types').Card>(`/issuing/cards/${id}`),
      create: (data: Partial<import('../types').Card>) =>
        request<import('../types').Card>('/issuing/cards', { method: 'POST', body: JSON.stringify(data) }),
      activate: (id: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/activate`, { method: 'POST' }),
      block: (id: string, reason: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/block`, { method: 'POST', body: JSON.stringify({ reason }) }),
      unblock: (id: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/unblock`, { method: 'POST' }),
      reportLost: (id: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/report-lost`, { method: 'POST' }),
      reportStolen: (id: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/report-stolen`, { method: 'POST' }),
      renew: (id: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/renew`, { method: 'POST' }),
      updateLimits: (id: string, limits: Record<string, number>) =>
        request<import('../types').Card>(`/issuing/cards/${id}/limits`, { method: 'PUT', body: JSON.stringify(limits) }),
      changePin: (id: string, pinBlock: string) =>
        request<import('../types').Card>(`/issuing/cards/${id}/pin`, { method: 'PUT', body: JSON.stringify({ pinBlock }) }),
      verifyPin: (id: string, pinBlock: string) =>
        request<{ verified: boolean }>(`/issuing/cards/${id}/pin/verify`, { method: 'POST', body: JSON.stringify({ pinBlock }) }),
      listByCardholder: (cardholderId: string) =>
        request<import('../types').Card[]>(`/issuing/cardholders/${cardholderId}/cards`),
    },
    tokens: {
      tokenize: (cardId: string, walletProvider: string, deviceId?: string) =>
        request<import('../types').WalletToken>('/issuing/tokens', { method: 'POST', body: JSON.stringify({ cardId, walletProvider, deviceId }) }),
      get: (tokenUuid: string) => request<import('../types').WalletToken>(`/issuing/tokens/${tokenUuid}`),
      suspend: (token: string) => request<import('../types').WalletToken>(`/issuing/tokens/${token}/suspend`, { method: 'POST' }),
      listByCard: (cardId: string) => request<import('../types').WalletToken[]>(`/issuing/cards/${cardId}/tokens`),
    },
  },
  acquiring: {
    merchants: {
      list: (status = 'ACTIVE') => request<import('../types').Merchant[]>(`/acquiring/merchants?status=${status}`),
      get: (id: string) => request<import('../types').Merchant>(`/acquiring/merchants/${id}`),
      getByCode: (code: string) => request<import('../types').Merchant>(`/acquiring/merchants/by-code/${code}`),
      create: (data: Partial<import('../types').Merchant>) =>
        request<import('../types').Merchant>('/acquiring/merchants', { method: 'POST', body: JSON.stringify(data) }),
      approve: (id: string) =>
        request<import('../types').Merchant>(`/acquiring/merchants/${id}/approve`, { method: 'POST' }),
      suspend: (id: string, reason: string) =>
        request<import('../types').Merchant>(`/acquiring/merchants/${id}/suspend`, { method: 'POST', body: JSON.stringify({ reason }) }),
      terminate: (id: string) =>
        request<import('../types').Merchant>(`/acquiring/merchants/${id}/terminate`, { method: 'POST' }),
    },
    terminals: {
      register: (data: Partial<import('../types').Terminal>) =>
        request<import('../types').Terminal>('/acquiring/terminals', { method: 'POST', body: JSON.stringify(data) }),
      get: (id: string) => request<import('../types').Terminal>(`/acquiring/terminals/${id}`),
      getByTid: (tid: string) => request<import('../types').Terminal>(`/acquiring/terminals/by-tid/${tid}`),
      listByMerchant: (merchantId: string) =>
        request<import('../types').Terminal[]>(`/acquiring/merchants/${merchantId}/terminals`),
      updateStatus: (id: string, status: string) =>
        request<import('../types').Terminal>(`/acquiring/terminals/${id}/status?status=${status}`, { method: 'PUT' }),
    },
    settlements: {
      get: (id: string) => request<import('../types').MerchantSettlement>(`/acquiring/settlements/${id}`),
      create: (merchantId: string, settlementDate: string, currencyCode: string) =>
        request<import('../types').MerchantSettlement>('/acquiring/settlements', { method: 'POST', body: JSON.stringify({ merchantId, settlementDate, currencyCode }) }),
      confirm: (id: string) =>
        request<import('../types').MerchantSettlement>(`/acquiring/settlements/${id}/confirm`, { method: 'POST' }),
      listByMerchant: (merchantId: string, from: string, to: string) =>
        request<import('../types').MerchantSettlement[]>(`/acquiring/merchants/${merchantId}/settlements?from=${from}&to=${to}`),
    },
  },
  authorization: {
    authorize: (data: Record<string, unknown>) =>
      request<import('../types').AuthDecision>('/authorization/authorize', { method: 'POST', body: JSON.stringify(data) }),
    rules: {
      list: () => request<import('../types').AuthRule[]>('/authorization/rules'),
      create: (data: Partial<import('../types').AuthRule>) =>
        request<import('../types').AuthRule>('/authorization/rules', { method: 'POST', body: JSON.stringify(data) }),
    },
    decisions: {
      list: (cardId: string, limit = 10) =>
        request<import('../types').AuthDecision[]>(`/authorization/decisions/${cardId}?limit=${limit}`),
    },
  },
  fraud: {
    evaluate: (data: Record<string, unknown>) =>
      request<{ decision: string; score: number }>('/fraud/evaluate', { method: 'POST', body: JSON.stringify(data) }),
    rules: {
      create: (data: Partial<import('../types').FraudRule>) =>
        request<import('../types').FraudRule>('/fraud/rules', { method: 'POST', body: JSON.stringify(data) }),
    },
    alerts: {
      list: (status = 'OPEN') => request<import('../types').FraudAlert[]>(`/fraud/alerts?status=${status}`),
      listByCard: (cardId: string) => request<import('../types').FraudAlert[]>(`/fraud/alerts/card/${cardId}`),
      confirm: (id: string) =>
        request<import('../types').FraudAlert>(`/fraud/alerts/${id}/confirm`, { method: 'POST' }),
      dismiss: (id: string) =>
        request<import('../types').FraudAlert>(`/fraud/alerts/${id}/dismiss`, { method: 'POST' }),
    },
  },
  clearing: {
    process: (data: Record<string, unknown>) =>
      request<import('../types').ClearingRecord>('/clearing/process', { method: 'POST', body: JSON.stringify(data) }),
    getByDate: (date: string) => request<import('../types').ClearingRecord[]>(`/clearing/by-date/${date}`),
    getByParticipant: (participantId: string) =>
      request<import('../types').ClearingRecord[]>(`/clearing/by-participant/${participantId}`),
    netting: {
      calculate: (date: string) =>
        request<import('../types').NettingRecord[]>(`/clearing/netting/calculate?date=${date}`, { method: 'POST' }),
    },
  },
  backoffice: {
    audit: {
      list: (resourceType?: string, resourceId?: string, limit = 50) => {
        const params = new URLSearchParams();
        if (resourceType) params.set('resourceType', resourceType);
        if (resourceId) params.set('resourceId', resourceId);
        params.set('limit', String(limit));
        return request<import('../types').AuditLog[]>(`/backoffice/audit?${params}`);
      },
      listByUser: (userId: string, limit = 20) =>
        request<import('../types').AuditLog[]>(`/backoffice/audit/user/${userId}?limit=${limit}`),
    },
    reports: {
      list: (type: string) => request<import('../types').Report[]>(`/backoffice/reports?type=${type}`),
      create: (data: Partial<import('../types').Report>) =>
        request<import('../types').Report>('/backoffice/reports', { method: 'POST', body: JSON.stringify(data) }),
    },
    monitoring: {
      alerts: () => request<import('../types').MonitoringEvent[]>('/backoffice/monitoring/alerts'),
      stats: (minutes = 60) => request<Record<string, number>>(`/backoffice/monitoring/stats?minutes=${minutes}`),
    },
  },
  acs: {
    authentications: {
      create: (data: Record<string, unknown>) =>
        request<import('../types').AcsAuthentication>('/acs/authentications', { method: 'POST', body: JSON.stringify(data) }),
      get: (id: string) => request<import('../types').AcsAuthentication>(`/acs/authentications/${id}`),
      listByCard: (cardId: string) => request<import('../types').AcsAuthentication[]>(`/acs/authentications/by-card/${cardId}`),
      requestChallenge: (id: string) =>
        request<import('../types').AcsAuthentication>(`/acs/authentications/${id}/challenge`, { method: 'POST' }),
      authenticate: (id: string, authenticationValue: string, eci: string) =>
        request<import('../types').AcsAuthentication>(`/acs/authentications/${id}/authenticate`, { method: 'POST', body: JSON.stringify({ authenticationValue, eci }) }),
      fail: (id: string) =>
        request<import('../types').AcsAuthentication>(`/acs/authentications/${id}/fail`, { method: 'POST' }),
    },
    challenges: {
      create: (authenticationId: string, challengeType: string) =>
        request<import('../types').AcsChallenge>('/acs/challenges', { method: 'POST', body: JSON.stringify({ authenticationId, challengeType }) }),
      verify: (id: string) =>
        request<import('../types').AcsChallenge>(`/acs/challenges/${id}/verify`, { method: 'POST' }),
    },
  },
  epg: {
    transactions: {
      initiate: (merchantId: string, merchantTransactionId: string, amount: number, currencyCode: string) =>
        request<import('../types').EpgTransaction>('/epg/transactions', { method: 'POST', body: JSON.stringify({ merchantId, merchantTransactionId, amount, currencyCode }) }),
      get: (id: string) => request<import('../types').EpgTransaction>(`/epg/transactions/${id}`),
      listByMerchant: (merchantId: string) =>
        request<import('../types').EpgTransaction[]>(`/epg/merchants/${merchantId}/transactions`),
      authorize: (id: string, cavv: string, eci: string) =>
        request<import('../types').EpgTransaction>(`/epg/transactions/${id}/authorize`, { method: 'POST', body: JSON.stringify({ cavv, eci }) }),
      capture: (id: string) =>
        request<import('../types').EpgTransaction>(`/epg/transactions/${id}/capture`, { method: 'POST' }),
      refund: (id: string) =>
        request<import('../types').EpgTransaction>(`/epg/transactions/${id}/refund`, { method: 'POST' }),
      fail: (id: string, errorCode: string, errorDescription: string) =>
        request<import('../types').EpgTransaction>(`/epg/transactions/${id}/fail`, { method: 'POST', body: JSON.stringify({ errorCode, errorDescription }) }),
    },
  },
  threeDs: {
    sessions: {
      create: (data: Record<string, unknown>) =>
        request<import('../types').ThreeDsSession>('/3dss/sessions', { method: 'POST', body: JSON.stringify(data) }),
      get: (id: string) => request<import('../types').ThreeDsSession>(`/3dss/sessions/${id}`),
      getByTxn: (transactionId: string) => request<import('../types').ThreeDsSession>(`/3dss/sessions/by-txn/${transactionId}`),
      complete: (id: string, authenticationValue: string) =>
        request<import('../types').ThreeDsSession>(`/3dss/sessions/${id}/complete`, { method: 'POST', body: JSON.stringify({ authenticationValue }) }),
      fail: (id: string, errorDescription: string) =>
        request<import('../types').ThreeDsSession>(`/3dss/sessions/${id}/fail`, { method: 'POST', body: JSON.stringify({ errorDescription }) }),
    },
  },
};
