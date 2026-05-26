export interface Participant {
  id: string;
  code: string;
  name: string;
  type: 'ACQUIRER' | 'ISSUER' | 'SWITCH' | 'PROCESSOR';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  endpointUrl?: string;
  endpointType?: 'TCP' | 'HTTP' | 'MQ' | 'FILE';
  supportedProtocols?: string[];
  metadata?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoutingRule {
  id: string;
  name: string;
  description?: string;
  priority: number;
  sourceParticipant?: Participant;
  destinationParticipant: Participant;
  conditionExpression: string;
  protocol: 'ISO8583' | 'ISO20022' | 'BOTH';
  messageType?: string;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface Transaction {
  id: string;
  transactionId: string;
  messageType?: string;
  protocol?: 'ISO8583' | 'ISO20022';
  stan?: string;
  rrn?: string;
  amount?: number;
  currencyCode?: string;
  merchantId?: string;
  terminalId?: string;
  status: 'PENDING' | 'ROUTING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'TIMEOUT' | 'REJECTED';
  responseCode?: string;
  processingTimeMs?: number;
  requestAt: string;
  responseAt?: string;
}

export interface DashboardStats {
  totalLastHour: number;
  totalLast24h: number;
  avgProcessingTimeMs: number;
  statusBreakdown: Record<string, number>;
  totalByStatus: Record<string, number>;
}

export interface BinTable {
  id: string;
  bin: string;
  binLength: number;
  participant: Participant;
  cardBrand?: 'VISA' | 'MASTERCARD' | 'AMEX' | 'CB' | 'OTHER';
  cardType?: 'CREDIT' | 'DEBIT' | 'PREPAID' | 'CHARGE';
  countryCode?: string;
  currencyCode?: string;
  isActive: boolean;
}
