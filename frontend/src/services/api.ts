const VITE_API_URL = (typeof import.meta !== 'undefined' && (import.meta as Record<string, any>).env?.VITE_API_URL) as string | undefined;
const BASE_URL = VITE_API_URL || '/api/v1';

// TODO: Migrate access token to httpOnly cookie for XSS protection
function getToken(): string | null {
  return sessionStorage.getItem('accessToken');
}

export async function safeRequest<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { ...headers, ...(options?.headers as Record<string, string>) },
    ...options,
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`API error ${res.status}: ${err}`);
  }
  return res.json();
}

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { ...headers, ...(options?.headers as Record<string, string>) },
    ...options,
  });

  if ((res.status === 401 || res.status === 403) && token) {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        const refreshRes = await fetch(`/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        if (refreshRes.ok) {
          const data = await refreshRes.json();
          sessionStorage.setItem('accessToken', data.accessToken);
          // TODO: Move refreshToken to httpOnly cookie
          localStorage.setItem('refreshToken', data.refreshToken);
          headers['Authorization'] = `Bearer ${data.accessToken}`;
          const retryRes = await fetch(`${BASE_URL}${path}`, {
            headers: { ...headers, ...(options?.headers as Record<string, string>) },
            ...options,
          });
          if (!retryRes.ok) {
            const err = await retryRes.text();
            throw new Error(`API error ${retryRes.status}: ${err}`);
          }
          return retryRes.json();
        }
      } catch {
        // refresh failed
      }
    }
    sessionStorage.removeItem('accessToken');
    // TODO: Move refreshToken to httpOnly cookie
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
  }

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`API error ${res.status}: ${err}`);
  }
  return res.json();
}

export const api = {
  liveConfig: {
    list: () => request<import('../types').LiveConfigItem[]>('/admin/live-config'),
    update: (id: string, value: string) =>
      request<import('../types').LiveConfigItem>(`/admin/live-config/${id}`, { method: 'PUT', body: JSON.stringify({ value }) }),
  },
  reports: {
    list: (type: string) => request<import('../types').Report[]>(`/backoffice/reports?type=${type}`),
    create: (data: Record<string, unknown>) =>
      request<import('../types').Report>('/backoffice/reports', { method: 'POST', body: JSON.stringify(data) }),
    generate: (id: string) =>
      request<import('../types').Report>(`/backoffice/reports/${id}/generate`, { method: 'POST' }),
  },
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
    update: (id: string, data: Partial<import('../types').RoutingRule>) =>
      request<import('../types').RoutingRule>(`/admin/routing-rules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => request<void>(`/admin/routing-rules/${id}`, { method: 'DELETE' }),
  },
  transactions: {
    list: (page = 0, size = 20, channel?: string, transactionType?: string, posEntryMode?: string) => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (channel) params.set('channel', channel);
      if (transactionType) params.set('transactionType', transactionType);
      if (posEntryMode) params.set('posEntryMode', posEntryMode);
      return request<{ content: import('../types').Transaction[] }>(`/switch/transactions?${params}`);
    },
    get: (id: string) => request<import('../types').Transaction>(`/switch/transactions/${id}`),
  },
  binTables: {
    list: () => request<import('../types').BinTable[]>('/admin/bin-tables'),
    get: (id: string) => request<import('../types').BinTable>(`/admin/bin-tables/${id}`),
    create: (data: Partial<import('../types').BinTable>) =>
      request<import('../types').BinTable>('/admin/bin-tables', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: string, data: Partial<import('../types').BinTable>) =>
      request<import('../types').BinTable>(`/admin/bin-tables/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => request<void>(`/admin/bin-tables/${id}`, { method: 'DELETE' }),
  },
  issuing: {
    cardholders: {
      list: () => request<import('../types').Cardholder[]>('/issuing/cardholders').then(r => ((r as any).content ?? r) as import('../types').Cardholder[]),
      get: (id: string) => request<import('../types').Cardholder>(`/issuing/cardholders/${id}`),
      getByEmail: (email: string) => request<import('../types').Cardholder>(`/issuing/cardholders/by-email/${email}`),
      create: (data: Partial<import('../types').Cardholder>) =>
        request<import('../types').Cardholder>('/issuing/cardholders', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').Cardholder>) =>
        request<import('../types').Cardholder>(`/issuing/cardholders/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      updateKyc: (id: string, level: number) =>
        request<import('../types').Cardholder>(`/issuing/cardholders/${id}/kyc?level=${level}`, { method: 'PUT' }),
      block: (id: string) =>
        request<import('../types').Cardholder>(`/issuing/cardholders/${id}/block`, { method: 'POST' }),
    },
    cards: {
      get: (id: string) => request<import('../types').Card>(`/issuing/cards/${id}`),
      getBySuffix: (suffix: string) => request<import('../types').Card>(`/issuing/cards/by-suffix/${suffix}`),
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
    pins: {
      setPin: (cardId: string, rawPin?: string, pinBlock?: string) =>
        request<{ message: string }>('/issuing/pins', { method: 'POST', body: JSON.stringify({ cardId, rawPin, pinBlock }) }),
      verifyPin: (cardId: string, pinBlock: string) =>
        request<{ verified: boolean }>('/issuing/pins/verify', { method: 'POST', body: JSON.stringify({ cardId, pinBlock }) }),
      changePin: (cardId: string, oldPinBlock: string, newPinBlock: string) =>
        request<{ changed: boolean }>('/issuing/pins', { method: 'PUT', body: JSON.stringify({ cardId, oldPinBlock, newPinBlock }) }),
    },
    tokenVault: {
      tokenize: (cardId: string, walletProvider?: string, deviceId?: string, fpan?: string) =>
        request<{ uuid: string; dpan: string; status: string }>('/issuing/tokens/tokenize', { method: 'POST', body: JSON.stringify({ cardId, walletProvider, deviceId, fpan }) }),
      getByUuid: (uuid: string) => request<{ uuid: string; dpan: string; status: string }>(`/issuing/tokens/uuid/${uuid}`),
      getByDpan: (dpan: string) => request<{ uuid: string; dpan: string; status: string }>(`/issuing/tokens/by-dpan/${dpan}`),
      suspend: (dpan: string) => request<{ uuid: string; dpan: string; status: string }>(`/issuing/tokens/${dpan}/suspend`, { method: 'POST' }),
      activate: (dpan: string) => request<{ uuid: string; dpan: string; status: string }>(`/issuing/tokens/${dpan}/activate`, { method: 'POST' }),
      delete: (dpan: string) => request<{ uuid: string; dpan: string; status: string }>(`/issuing/tokens/${dpan}/delete`, { method: 'POST' }),
      listByCard: (cardId: string) => request<{ uuid: string; dpan: string; status: string }[]>(`/issuing/tokens/by-card/${cardId}`),
    },
    accounts: {
      list: () => request<import('../types').CardAccount[]>('/issuing/accounts'),
      get: (id: string) => request<import('../types').CardAccount>(`/issuing/accounts/${id}`),
      listByCardholder: (cardholderId: string) =>
        request<import('../types').CardAccount[]>(`/issuing/cardholders/${cardholderId}/accounts`),
      create: (data: Partial<import('../types').CardAccount>) =>
        request<import('../types').CardAccount>('/issuing/accounts', { method: 'POST', body: JSON.stringify(data) }),
      debit: (id: string, amount: number, currencyCode: string) =>
        request<import('../types').CardAccount>(`/issuing/accounts/${id}/debit`, { method: 'POST', body: JSON.stringify({ amount, currencyCode }) }),
      credit: (id: string, amount: number, currencyCode: string) =>
        request<import('../types').CardAccount>(`/issuing/accounts/${id}/credit`, { method: 'POST', body: JSON.stringify({ amount, currencyCode }) }),
      hold: (id: string, amount: number) =>
        request<import('../types').CardAccount>(`/issuing/accounts/${id}/hold`, { method: 'POST', body: JSON.stringify({ amount }) }),
      releaseHold: (id: string, amount: number) =>
        request<import('../types').CardAccount>(`/issuing/accounts/${id}/release-hold`, { method: 'POST', body: JSON.stringify({ amount }) }),
    },
    notifications: {
      list: () => request<import('../types').Notification[]>('/issuing/notifications'),
      listByCardholder: (cardholderId: string) =>
        request<import('../types').Notification[]>(`/issuing/notifications/by-cardholder/${cardholderId}`),
      listByCard: (cardId: string) =>
        request<import('../types').Notification[]>(`/issuing/notifications/by-card/${cardId}`),
    },
  },
  acquiring: {
    merchants: {
      list: (status = 'ACTIVE') => request<import('../types').Merchant[]>(`/acquiring/merchants?status=${status}`),
      get: (id: string) => request<import('../types').Merchant>(`/acquiring/merchants/${id}`),
      getByCode: (code: string) => request<import('../types').Merchant>(`/acquiring/merchants/by-code/${code}`),
      create: (data: Partial<import('../types').Merchant>) =>
        request<import('../types').Merchant>('/acquiring/merchants', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').Merchant>) =>
        request<import('../types').Merchant>(`/acquiring/merchants/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
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
      injectKeys: (tid: string, mKey: string, pik: string, mak: string) =>
        request<import('../types').Terminal>(`/acquiring/terminals/${tid}/keys`, { method: 'PUT', body: JSON.stringify({ mKey, pik, mak }) }),
      rotateKeys: (tid: string) =>
        request<import('../types').Terminal>(`/acquiring/terminals/${tid}/keys/rotate`, { method: 'POST' }),
    },
    settlements: {
      get: (id: string) => request<import('../types').MerchantSettlement>(`/acquiring/settlements/${id}`),
      create: (merchantId: string, settlementDate: string, currencyCode: string) =>
        request<import('../types').MerchantSettlement>('/acquiring/settlements', { method: 'POST', body: JSON.stringify({ merchantId, settlementDate, currencyCode }) }),
      confirm: (id: string) =>
        request<import('../types').MerchantSettlement>(`/acquiring/settlements/${id}/confirm`, { method: 'POST' }),
      markPaid: (id: string, paymentRef: string) =>
        request<import('../types').MerchantSettlement>(`/acquiring/settlements/${id}/pay?paymentRef=${paymentRef}`, { method: 'POST' }),
      listByMerchant: (merchantId: string, from: string, to: string) =>
        request<import('../types').MerchantSettlement[]>(`/acquiring/merchants/${merchantId}/settlements?from=${from}&to=${to}`),
    },
    netting: {
      calculate: (merchantId: string, date: string) =>
        request<Record<string, unknown>>(`/acquiring/merchants/${merchantId}/netting?date=${date}`),
    },
  },
  authorization: {
    authorize: (data: Record<string, unknown>) =>
      request<import('../types').AuthDecision>('/authorization/authorize', { method: 'POST', body: JSON.stringify(data) }),
    rules: {
      list: () => request<import('../types').AuthRule[]>('/authorization/rules'),
      create: (data: Partial<import('../types').AuthRule>) =>
        request<import('../types').AuthRule>('/authorization/rules', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').AuthRule>) =>
        request<import('../types').AuthRule>(`/authorization/rules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      remove: (id: string) =>
        request<void>(`/authorization/rules/${id}`, { method: 'DELETE' }),
    },
    decisions: {
      list: (cardId: string, limit = 10) =>
        request<import('../types').AuthDecision[]>(`/authorization/decisions/${cardId}?limit=${limit}`),
      getByTransaction: (transactionId: string) =>
        request<import('../types').AuthDecision>(`/authorization/decisions/by-transaction/${transactionId}`),
    },
    holds: {
      listByCard: (cardId: string) =>
        request<import('../types').HoldRecord[]>(`/authorization/holds/card/${cardId}`),
      listByAccount: (accountId: string) =>
        request<import('../types').HoldRecord[]>(`/authorization/holds/account/${accountId}`),
      release: (holdId: string) =>
        request<void>(`/authorization/holds/${holdId}/release`, { method: 'POST' }),
      capture: (holdId: string) =>
        request<void>(`/authorization/holds/${holdId}/capture`, { method: 'POST' }),
    },
    simulate: (data: Record<string, unknown>) =>
      request<import('../types').AuthDecision>('/authorization/authorize', { method: 'POST', body: JSON.stringify(data) }),
  },
  fraud: {
    evaluate: (data: Record<string, unknown>) =>
      request<{ decision: string; score: number }>('/fraud/evaluate', { method: 'POST', body: JSON.stringify(data) }),
    rules: {
      list: () => request<import('../types').FraudRule[]>('/fraud/rules'),
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
    interchange: {
      list: () => request<import('../types').InterchangeFee[]>('/clearing/interchange'),
      update: (id: string, data: Record<string, unknown>) =>
        request<import('../types').InterchangeFee>(`/clearing/interchange/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/clearing/interchange/${id}`, { method: 'DELETE' }),
    },
    files: {
      generateOutgoing: (date: string, participantId: string, format = 'CSV') =>
        request<string>(`/clearing/files/outgoing?date=${date}&participantId=${participantId}&format=${format}`),
      uploadIncoming: (content: string, format = 'CSV', participantId?: string) =>
        request<import('../types').ReconciliationResult>('/clearing/files/incoming', { method: 'POST', body: JSON.stringify({ content, format, participantId }) }),
      downloadBct: (date: string) =>
        request<string>(`/clearing/files/bct?date=${date}`),
    },
    reconciliation: {
      list: (date?: string) =>
        request<import('../types').ReconciliationRecord[]>(`/clearing/reconciliation${date ? `?date=${date}` : ''}`),
    },
    reports: {
      quarterly: (year: number, quarter: number, scheme = 'ALL') =>
        request<string>(`/clearing/reports/quarterly?year=${year}&quarter=${quarter}&scheme=${scheme}`),
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
    enrollments: {
      list: () => safeRequest<import('../types').AcsEnrollment[]>('/acs/enrollments'),
      get: (id: string) => safeRequest<import('../types').AcsEnrollment>(`/acs/enrollments/${id}`),
      enroll: (data: Record<string, unknown>) =>
        safeRequest<import('../types').AcsEnrollment>('/acs/enrollments', { method: 'POST', body: JSON.stringify(data) }),
      unenroll: (id: string) =>
        safeRequest<import('../types').AcsEnrollment>(`/acs/enrollments/${id}/unenroll`, { method: 'POST' }),
    },
    authentications: {
      create: (data: Record<string, unknown>) =>
        request<import('../types').AcsAuthentication>('/acs/authentications', { method: 'POST', body: JSON.stringify(data) }),
      get: (id: string) => request<import('../types').AcsAuthentication>(`/acs/authentications/${id}`),
      list: () => safeRequest<import('../types').AcsAuthentication[]>('/acs/authentications'),
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
    merchants: {
      list: () => safeRequest<import('../types').EpgMerchantConfig[]>('/epg/merchants'),
      get: (id: string) => safeRequest<import('../types').EpgMerchantConfig>(`/epg/merchants/${id}`),
      create: (data: Record<string, unknown>) =>
        safeRequest<import('../types').EpgMerchantConfig>('/epg/merchants', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Record<string, unknown>) =>
        safeRequest<import('../types').EpgMerchantConfig>(`/epg/merchants/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => safeRequest<void>(`/epg/merchants/${id}`, { method: 'DELETE' }),
    },
    transactions: {
      initiate: (merchantId: string, merchantTransactionId: string, amount: number, currencyCode: string) =>
        request<import('../types').EpgTransaction>('/epg/transactions', { method: 'POST', body: JSON.stringify({ merchantId, merchantTransactionId, amount, currencyCode }) }),
      get: (id: string) => request<import('../types').EpgTransaction>(`/epg/transactions/${id}`),
      list: () => safeRequest<import('../types').EpgTransaction[]>('/epg/transactions'),
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
      list: () => safeRequest<import('../types').ThreeDsSession[]>('/3dss/sessions'),
      getByTxn: (transactionId: string) => request<import('../types').ThreeDsSession>(`/3dss/sessions/by-txn/${transactionId}`),
      complete: (id: string, authenticationValue: string) =>
        request<import('../types').ThreeDsSession>(`/3dss/sessions/${id}/complete`, { method: 'POST', body: JSON.stringify({ authenticationValue }) }),
      cancel: (id: string) =>
        safeRequest<import('../types').ThreeDsSessionCancelResponse>(`/3dss/sessions/${id}/cancel`, { method: 'POST' }),
      fail: (id: string, errorDescription: string) =>
        request<import('../types').ThreeDsSession>(`/3dss/sessions/${id}/fail`, { method: 'POST', body: JSON.stringify({ errorDescription }) }),
    },
  },
  disputes: {
    list: (status?: string, merchantId?: string) => {
      const params = new URLSearchParams();
      if (status) params.set('status', status);
      if (merchantId) params.set('merchantId', merchantId);
      const qs = params.toString();
      return safeRequest<import('../types').Dispute[]>(`/disputes${qs ? '?' + qs : ''}`);
    },
    get: (id: string) => safeRequest<{ dispute: import('../types').Dispute; timeline: import('../types').DisputeTimeline[] }>(`/disputes/${id}`),
    open: (data: Record<string, unknown>) => safeRequest<import('../types').Dispute>('/disputes', { method: 'POST', body: JSON.stringify(data) }),
    transition: (id: string, status: string, notes?: string) =>
      safeRequest<import('../types').Dispute>(`/disputes/${id}/transition`, { method: 'POST', body: JSON.stringify({ status, notes }) }),
    submitEvidence: (id: string, data: Record<string, unknown>) =>
      safeRequest<import('../types').DisputeEvidence>(`/disputes/${id}/evidence`, { method: 'POST', body: JSON.stringify(data) }),
    getEvidence: (id: string) => safeRequest<import('../types').DisputeEvidence[]>(`/disputes/${id}/evidence`),
    getTimeline: (id: string) => safeRequest<import('../types').DisputeTimeline[]>(`/disputes/${id}/timeline`),
    getByTransaction: (transactionId: string) => safeRequest<import('../types').Dispute[]>(`/disputes/transaction/${transactionId}`),
  },
  get: <T = unknown>(path: string) => request<T>(path),
  post: <T = unknown>(path: string, body?: Record<string, unknown>) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  delete: <T = unknown>(path: string) => request<T>(path, { method: 'DELETE' }),
  auth: {
    login: (data: import('../types').LoginRequest) =>
      request<import('../types').LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
    register: (data: import('../types').RegisterRequest) =>
      request<import('../types').AuthUser>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
    refresh: (refreshToken: string) =>
      request<import('../types').LoginResponse>('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
    me: () => request<import('../types').AuthUser>('/auth/me'),
    users: {
      list: () => request<import('../types').AuthUser[]>('/auth/users'),
      update: (id: string, data: Partial<import('../types').AuthUser>) =>
        request<import('../types').AuthUser>(`/auth/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/auth/users/${id}`, { method: 'DELETE' }),
    },
    mfa: {
      setup: (username?: string) =>
        request<import('../types').MfaSetupData>('/auth/mfa/setup', { method: 'POST', body: JSON.stringify({ username }) }),
      verify: (username: string, code: string) =>
        request<{ enabled: boolean }>('/auth/mfa/verify', { method: 'POST', body: JSON.stringify({ username, code }) }),
      disable: (username: string, code: string) =>
        request<{ disabled: boolean }>('/auth/mfa/disable', { method: 'POST', body: JSON.stringify({ username, code }) }),
      authenticate: (username: string, code: string) =>
        request<import('../types').LoginResponse>('/auth/mfa/authenticate', { method: 'POST', body: JSON.stringify({ username, code }) }),
    },
  },
  standin: {
    rules: {
      list: () => request<import('../types').StandInRule[]>('/standin/rules'),
      create: (data: Partial<import('../types').StandInRule>) =>
        request<import('../types').StandInRule>('/standin/rules', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').StandInRule>) =>
        request<import('../types').StandInRule>(`/standin/rules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/standin/rules/${id}`, { method: 'DELETE' }),
    },
    authorizations: {
      list: (issuerId?: string) =>
        request<import('../types').StandInAuthorization[]>(`/standin/authorizations${issuerId ? `?issuerId=${issuerId}` : ''}`),
    },
    pendingCount: () => request<{ count: number }>('/standin/pending/count'),
  },
  cof: {
    tokens: {
      list: (participantId?: string) =>
        request<import('../types').CofToken[]>(`/ecommerce/cof/tokens${participantId ? `?participantId=${participantId}` : ''}`),
      get: (id: string) => request<import('../types').CofToken>(`/ecommerce/cof/tokens/${id}`),
      create: (data: Partial<import('../types').CofToken>) =>
        request<import('../types').CofToken>('/ecommerce/cof/tokens', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').CofToken>) =>
        request<import('../types').CofToken>(`/ecommerce/cof/tokens/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/ecommerce/cof/tokens/${id}`, { method: 'DELETE' }),
    },
    schedules: {
      list: (cofTokenId?: string) =>
        request<import('../types').RecurringSchedule[]>(`/ecommerce/cof/schedules${cofTokenId ? `?cofTokenId=${cofTokenId}` : ''}`),
      get: (id: string) => request<import('../types').RecurringSchedule>(`/ecommerce/cof/schedules/${id}`),
      create: (data: Partial<import('../types').RecurringSchedule>) =>
        request<import('../types').RecurringSchedule>('/ecommerce/cof/schedules', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').RecurringSchedule>) =>
        request<import('../types').RecurringSchedule>(`/ecommerce/cof/schedules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/ecommerce/cof/schedules/${id}`, { method: 'DELETE' }),
    },
  },
  fx: {
    rates: {
      list: () => request<import('../types').FxRate[]>('/fx/rates'),
      get: (id: string) => request<import('../types').FxRate>(`/fx/rates/${id}`),
      create: (data: Partial<import('../types').FxRate>) =>
        request<import('../types').FxRate>('/fx/rates', { method: 'POST', body: JSON.stringify(data) }),
      update: (id: string, data: Partial<import('../types').FxRate>) =>
        request<import('../types').FxRate>(`/fx/rates/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (id: string) => request<void>(`/fx/rates/${id}`, { method: 'DELETE' }),
    },
    convert: (amount: number, sourceCurrency: string, targetCurrency: string) =>
      request<{ amount: number; sourceCurrency: string; targetCurrency: string; convertedAmount: number }>(
        '/fx/convert', { method: 'POST', body: JSON.stringify({ amount, sourceCurrency, targetCurrency }) }),
    proposeDcc: (amount: number, sourceCurrency: string, targetCurrency: string) =>
      request<{ amount: number; sourceCurrency: string; targetCurrency: string; dccAmount: number }>(
        '/fx/dcc/propose', { method: 'POST', body: JSON.stringify({ amount, sourceCurrency, targetCurrency }) }),
  },
  regulatory: {
    reports: {
      listTemplates: () => request<import('../types').RegulatoryReportTemplate[]>('/regulatory/reports'),
      generate: (templateId: string, startDate: string, endDate: string, format = 'CSV') =>
        request<string>('/regulatory/reports/generate', { method: 'POST', body: JSON.stringify({ templateId, startDate, endDate, format }) }),
    },
  },
  credit: {
    lines: {
      open: (data: Record<string, unknown>) =>
        request<import('../types').CreditLine>('/credit/lines', { method: 'POST', body: JSON.stringify(data) }),
      get: (id: string) => request<import('../types').CreditLine>(`/credit/lines/${id}`),
      getByCardAccount: (cardAccountId: string) =>
        request<import('../types').CreditLine>(`/credit/lines/by-card-account/${cardAccountId}`),
      authorize: (id: string, amount: number) =>
        request<import('../types').CreditLine>(`/credit/lines/${id}/authorize`, { method: 'POST', body: JSON.stringify({ amount }) }),
      purchase: (id: string, amount: number, transactionRef: string) =>
        request<import('../types').CreditLine>(`/credit/lines/${id}/purchase`, { method: 'POST', body: JSON.stringify({ amount, transactionRef }) }),
      payment: (id: string, amount: number) =>
        request<import('../types').CreditLine>(`/credit/lines/${id}/payment`, { method: 'POST', body: JSON.stringify({ amount }) }),
      releaseHold: (id: string, amount: number) =>
        request<import('../types').CreditLine>(`/credit/lines/${id}/release-hold`, { method: 'POST', body: JSON.stringify({ amount }) }),
      simulate: (id: string, balance: number, apr: number, minPaymentPct: number, minPaymentFloor: number) =>
        request<{ monthlyRate: number; interestCharged: number; minimumPayment: number; newBalance: number }>(
          `/credit/lines/${id}/simulate`, { method: 'POST', body: JSON.stringify({ balance, apr, minPaymentPct, minPaymentFloor }) }),
      statements: (id: string) =>
        request<import('../types').CreditStatement[]>(`/credit/lines/${id}/statements`),
      generateStatement: (id: string) =>
        request<import('../types').CreditStatement>(`/credit/lines/${id}/generate-statement`, { method: 'POST' }),
      installments: (id: string) =>
        request<import('../types').InstallmentPlan[]>(`/credit/lines/${id}/installments`),
      convertToInstallments: (id: string, data: Record<string, unknown>) =>
        request<import('../types').InstallmentPlan>(`/credit/lines/${id}/installments`, { method: 'POST', body: JSON.stringify(data) }),
    },
    statements: {
      get: (id: string) => request<import('../types').CreditStatement>(`/credit/statements/${id}`),
    },
    installmentPlans: {
      get: (id: string) => request<import('../types').InstallmentPlan>(`/credit/installment-plans/${id}`),
      entries: (id: string) => request<import('../types').InstallmentEntry[]>(`/credit/installment-plans/${id}/entries`),
    },
    installmentEntries: {
      markPaid: (id: string, statementId: string) =>
        request<import('../types').InstallmentEntry>(`/credit/installment-entries/${id}/pay`, { method: 'POST', body: JSON.stringify({ statementId }) }),
    },
  },
};
