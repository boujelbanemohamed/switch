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

// Issuing
export interface Cardholder {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  status: string;
  kycLevel: number;
  dateOfBirth?: string;
  nationality?: string;
  createdAt: string;
}

export interface Card {
  id: string;
  cardholderId: string;
  panSuffix: string;
  cardBrand: string;
  cardType: string;
  status: string;
  dailyLimit: number;
  weeklyLimit: number;
  monthlyLimit: number;
  singleTxnLimit: number;
  createdAt: string;
  expiresAt?: string;
}

export interface WalletToken {
  id: string;
  cardId: string;
  token: string;
  walletProvider: string;
  deviceId?: string;
  status: string;
  createdAt: string;
}

export interface CardAccount {
  id: string;
  cardId: string;
  cardholderId: string;
  balance: number;
  availableBalance: number;
  holdAmount: number;
  currencyCode: string;
  status: string;
  createdAt: string;
}

export interface Notification {
  id: string;
  cardId: string;
  cardholderId: string;
  type: string;
  message: string;
  actionRequired: boolean;
  createdAt: string;
}

// Acquiring
export interface Merchant {
  id: string;
  merchantId: string;
  code: string;
  tradingName?: string;
  legalName?: string;
  name: string;
  categoryCode?: string;
  merchantCategoryCode?: string;
  status: string;
  countryCode?: string;
  currencyCode?: string;
  email?: string;
  phone?: string;
  addressLine1?: string;
  mdrPlan?: MdrPlan;
  acquiringParticipant?: Participant;
  createdAt: string;
}

export interface MdrPlan {
  id: string;
  name: string;
  rate: number;
  fixedFee: number;
  currencyCode: string;
}

export interface Terminal {
  id: string;
  terminalId: string;
  merchantId: string;
  serialNumber?: string;
  terminalType?: string;
  type?: string;
  manufacturer?: string;
  model?: string;
  status: string;
  location?: string;
  createdAt: string;
}

export interface MerchantSettlement {
  id: string;
  merchantId: string;
  settlementDate: string;
  totalAmount: number;
  totalFee: number;
  totalFees?: number;
  netAmount: number;
  currencyCode: string;
  status: string;
  paymentRef?: string;
}

// Authorization
export interface Condition {
  field: 'amount' | 'currency' | 'merchantCategory' | 'cardType' | 'country' | 'transactionType' | 'channel' | 'timeOfDay' | 'dayOfWeek';
  operator: 'GREATER_THAN' | 'LESS_THAN' | 'EQUAL' | 'NOT_EQUAL' | 'IN' | 'NOT_IN' | 'BETWEEN';
  value: string;
}

export type ConditionField = Condition['field'];
export type ConditionOperator = Condition['operator'];

export const CONDITION_FIELDS: ConditionField[] = [
  'amount', 'currency', 'merchantCategory', 'cardType',
  'country', 'transactionType', 'channel', 'timeOfDay', 'dayOfWeek',
];

export const OPERATORS_BY_FIELD: Record<ConditionField, ConditionOperator[]> = {
  amount: ['GREATER_THAN', 'LESS_THAN', 'EQUAL', 'BETWEEN'],
  currency: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  merchantCategory: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  cardType: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  country: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  transactionType: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  channel: ['EQUAL', 'NOT_EQUAL', 'IN', 'NOT_IN'],
  timeOfDay: ['BETWEEN', 'EQUAL'],
  dayOfWeek: ['BETWEEN', 'EQUAL'],
};

export const IS_MULTI_VALUE_OP: ConditionOperator[] = ['IN', 'NOT_IN'];
export const IS_RANGE_OP: ConditionOperator[] = ['BETWEEN'];

export interface HoldRecord {
  id: string;
  cardId?: string;
  accountId?: string;
  amount: number;
  reason?: string;
  status: string;
  expiresAt?: string;
  createdAt: string;
}

export interface AuthRule {
  id: string;
  name: string;
  ruleType: string;
  action: string;
  conditionExpression: string;
  priority: number;
  status: string;
  successCount: number;
  failureCount: number;
}

export interface AuthDecision {
  id: string;
  cardId: string;
  transactionId: string;
  decision: string;
  reason?: string;
  responseCode: string;
  timestamp: string;
}

// Fraud
export interface FraudRule {
  id: string;
  name: string;
  description?: string;
  ruleCategory: 'VELOCITY' | 'GEO' | 'BEHAVIORAL' | 'AMOUNT' | 'MERCHANT' | 'DEVICE' | 'NETWORK' | 'ML_MODEL' | 'MANUAL';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  action: 'BLOCK' | 'FLAG' | 'CHALLENGE' | 'MONITOR' | 'TFA' | 'ALLOW';
  conditionExpression?: string;
  scoreWeight?: number;
  cooldownSeconds?: number;
  status: 'ACTIVE' | 'INACTIVE' | 'TESTING';
  falsePositiveCount: number;
  truePositiveCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface FraudAlert {
  id: string;
  cardId: string;
  transactionId: string;
  ruleName: string;
  score: number;
  status: string;
  decision?: string;
  description?: string;
  createdAt: string;
}

// Clearing
export interface ClearingRecord {
  id: string;
  transactionId: string;
  participantId: string;
  amount: number;
  fee: number;
  netAmount: number;
  currencyCode: string;
  status: string;
  clearingDate: string;
  disputeReason?: string;
}

export interface NettingRecord {
  id: string;
  participantId: string;
  grossDebit: number;
  grossCredit: number;
  netAmount: number;
  currencyCode: string;
  status: string;
  settlementRef?: string;
}

// Backoffice
export interface AuditLog {
  id: string;
  userId?: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details?: string;
  status: string;
  createdAt: string;
}

export interface Report {
  id: string;
  type: string;
  name: string;
  parameters: string;
  status: string;
  resultSummary?: string;
  createdAt: string;
}

export interface MonitoringEvent {
  id: number;
  eventType: string;
  severity: string;
  source: string;
  message: string;
  metricValue?: number;
  threshold?: number;
  acknowledged: boolean;
  acknowledgedBy?: string;
  createdAt: string;
}

// Auth
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  displayName?: string;
  role: 'ADMIN' | 'OPERATOR' | 'ANALYST' | 'AUDITOR' | 'VIEWER';
  enabled: boolean;
  lastLogin?: string;
  createdAt: string;
  mfaEnabled?: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
  displayName: string;
  email: string;
  tokenType: string;
  mfaRequired?: boolean;
}

export interface MfaSetupData {
  secret: string;
  uri: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  displayName?: string;
  role?: string;
}

// E-commerce
export interface AcsAuthentication {
  id: string;
  transactionId: string;
  cardId?: string;
  panHash?: string;
  merchantName?: string;
  amount: number;
  currencyCode: string;
  status: string;
  authenticationValue?: string;
  eci?: string;
  threeDsVersion: string;
  createdAt: string;
}

export interface AcsChallenge {
  id: string;
  authenticationId: string;
  challengeType: string;
  status: string;
  attempts: number;
  maxAttempts: number;
  expiresAt: string;
  verifiedAt?: string;
  createdAt: string;
}

export interface AcsEnrollment {
  id: string;
  cardId: string;
  cardholderId?: string;
  merchantId?: string;
  status: string;
  enrolledAt: string;
  cardBrand?: string;
  cardType?: string;
  phoneNumber?: string;
  email?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface EpgMerchantConfig {
  id: string;
  merchantId: string;
  isActive: boolean;
  apiKeyHash?: string;
  apiSecretHash?: string;
  webhookUrl?: string;
  callbackUrl?: string;
  allowedCurrencies?: string;
  allowedCardBrands?: string;
  minAmount?: number;
  maxAmount?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface EpgTransaction {
  id: string;
  merchantId: string;
  merchantTransactionId: string;
  amount: number;
  currencyCode: string;
  status: string;
  threeDsRequired: boolean;
  threeDsStatus?: string;
  cavv?: string;
  eci?: string;
  errorCode?: string;
  createdAt: string;
}

export interface ThreeDsSession {
  id: string;
  transactionId: string;
  threeDsVersion: string;
  status: string;
  authenticationValue?: string;
  eci?: string;
  acsUrl?: string;
  acsTransId?: string;
  dsTransId?: string;
  notificationUrl?: string;
  createdAt: string;
}

export interface ThreeDsSessionCancelResponse {
  id: string;
  status: string;
  message?: string;
}
