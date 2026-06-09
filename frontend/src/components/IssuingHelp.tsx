import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const CARD_STATUS_LABELS: Record<string, string> = {
  PENDING_ACTIVATION: 'En attente d\'activation',
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  BLOCKED: 'Bloquée',
  SUSPENDED: 'Suspendue',
  EXPIRED: 'Expirée',
  STOLEN: 'Volée',
  LOST: 'Perdue',
  DAMAGED: 'Endommagée',
  CLOSED: 'Clôturée',
  RENEWED: 'Renouvelée',
};

export const ACCOUNT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  BLOCKED: 'Bloqué',
  CLOSED: 'Clôturé',
};

export const CARDHOLDER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  BLOCKED: 'Bloqué',
  DECEASED: 'Décédé',
};

export const TOKEN_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  TERMINATED: 'Révoqué',
};

export const CARD_PRODUCT_LABELS: Record<string, string> = {
  CREDIT: 'Crédit',
  DEBIT: 'Débit',
  PREPAID: 'Prépayée',
  CHARGE: 'Charge',
};

export const WALLET_PROVIDER_LABELS: Record<string, string> = {
  APPLE_PAY: 'Apple Pay',
  GOOGLE_PAY: 'Google Pay',
  SAMSUNG_PAY: 'Samsung Pay',
  OTHER: 'Autre',
};

export const CARD_ACTION_LABELS: Record<string, string> = {
  activate: 'Activer',
  block: 'Bloquer',
  unblock: 'Débloquer',
  lost: 'Perdue',
  stolen: 'Volée',
  renew: 'Renouveler',
};

export const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  CARD_BLOCKED: 'Carte bloquée',
  CARD_UNBLOCKED: 'Carte débloquée',
  CARD_ACTIVATED: 'Carte activée',
  CARD_LOST: 'Carte perdue',
  CARD_STOLEN: 'Carte volée',
  CARD_RENEWED: 'Carte renouvelée',
  CARD_EXPIRED: 'Carte expirée',
  PIN_CHANGED: 'PIN modifié',
  ACCOUNT_CREDITED: 'Compte crédité',
  ACCOUNT_DEBITED: 'Compte débité',
  HOLD_PLACED: 'Réservation placée',
  HOLD_RELEASED: 'Réservation libérée',
};

export function getNotificationLabel(type: string): string {
  const upper = type.toUpperCase();
  if (upper.includes('STOLEN')) return 'Carte volée';
  if (upper.includes('BLOCK')) return 'Carte bloquée';
  if (upper.includes('LOST')) return 'Carte perdue';
  if (upper.includes('EXPIR')) return 'Carte expirée';
  if (upper.includes('ACTIVAT')) return 'Carte activée';
  if (upper.includes('RENEW')) return 'Carte renouvelée';
  if (upper.includes('CREDIT')) return 'Compte crédité';
  if (upper.includes('DEBIT')) return 'Compte débité';
  if (upper.includes('HOLD')) return 'Réservation';
  if (upper.includes('PIN')) return 'PIN modifié';
  return NOTIFICATION_TYPE_LABELS[type] ?? type;
}

export function IssuingHelp() {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        title="Aide"
        className="p-1.5 rounded-lg bg-gray-800 border border-gray-700 text-gray-300 hover:bg-gray-700 hover:text-white transition-colors"
      >
        <HelpCircle className="w-5 h-5" />
      </button>

      {open && (
        <div className="fixed inset-0 z-[2000] flex justify-end">
          <div className="absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
          <div
            className="relative w-[520px] max-w-full h-full bg-[var(--bg,#1e293b)] text-[var(--text,#e2e8f0)] overflow-y-auto shadow-2xl border-l border-[var(--border,#334155)]"
            style={{ animation: 'slideIn 0.2s ease-out' }}
          >
            <style>{`
              @keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }
            `}</style>

            <div className="sticky top-0 z-10 flex items-center justify-between p-4 border-b border-[var(--border,#334155)] bg-[var(--surface,#0f172a)]">
              <h2 className="text-lg font-bold">Aide — Émission (Issuing)</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère le cycle de vie des <strong>porteurs (cardholders)</strong>,
                  des <strong>cartes</strong> et des <strong>comptes</strong> associés. Elle permet
                  de créer et gérer des porteurs, d'émettre des cartes (physiques ou virtuelles),
                  d'activer/bloquer/renouveler des cartes, de créditer/débiter des comptes, de
                  gérer les PIN et de tokeniser des cartes pour les wallets mobiles (Apple Pay,
                  Google Pay).
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Porteur (Cardholder) :</strong> personne physique ou morale titulaire
                    d'un ou plusieurs comptes et cartes. Chaque porteur a un niveau KYC (Know Your
                    Customer) qui détermine ses plafonds.
                  </li>
                  <li>
                    <strong>Carte :</strong> moyen de paiement lié à un porteur et à un compte.
                    Une carte a un statut qui évolue dans le temps : activation, blocage, perte,
                    renouvellement, expiration.
                  </li>
                  <li>
                    <strong>Compte :</strong> support financier associé aux cartes. Un compte a un
                    solde, un solde disponible (après déduction des réservations/holds) et un
                    montant en hold.
                  </li>
                  <li>
                    <strong>Hold / Réservation :</strong> montant temporairement bloqué sur le
                    compte lors d'une autorisation. Libéré ou capturé selon l'issue de la
                    transaction. Géré manuellement via les actions « Place Hold » et
                    « Release Hold ».
                  </li>
                  <li>
                    <strong>PIN :</strong> code secret de la carte. Peut être défini, vérifié
                    ou changé. Le PIN est stocké sous forme de bloc chiffré (pin block) — on ne
                    peut pas le lire en clair après création.
                  </li>
                  <li>
                    <strong>Tokenisation :</strong> remplacement du PAN (numéro de carte réel)
                    par un DPAN (token) utilisé par les wallets (Apple Pay, Google Pay). Le token
                    peut être activé ou suspendu sans affecter la carte physique.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Créer un porteur :</strong> dans l'onglet Cardholders, cliquez sur
                    « Create Cardholder ». Remplissez les informations (nom, email, téléphone,
                    document d'identité) et validez.
                  </li>
                  <li>
                    <strong>Sélectionner un porteur :</strong> cliquez sur une ligne dans le
                    tableau Cardholders pour charger ses cartes, comptes et notifications.
                  </li>
                  <li>
                    <strong>Créer une carte :</strong> sélectionnez un porteur, allez dans
                    l'onglet Cards, cliquez sur « Create Card ». Choisissez le produit
                    (Crédit/Débit/Prépayée), saisissez le numéro de carte, la date d'expiration
                    et le CVV.
                  </li>
                  <li>
                    <strong>Activer / Bloquer / Déclarer perdue :</strong> utilisez les boutons
                    d'action dans la colonne Actions du tableau des cartes. Activer passe la carte
                    en statut « Active ». Bloquer la passe en « Bloquée ». Perdue/Volée la passe
                    en « Perdue »/« Volée ». Renouveler crée une nouvelle carte avec un nouveau
                    numéro.
                  </li>
                  <li>
                    <strong>Créditer / Débiter un compte :</strong> dans l'onglet Accounts,
                    saisissez un montant et cliquez sur Credit (solde augmente) ou Debit (solde
                    diminue). Place Hold réserve le montant, Release Hold libère la réservation.
                  </li>
                  <li>
                    <strong>Gérer le PIN :</strong> dans l'onglet PIN, sélectionnez une carte
                    dans la liste. Saisissez le PIN en clair et un pin block pour définir un
                    nouveau PIN. Utilisez Verify pour vérifier un PIN existant, Change pour le
                    modifier (ancien + nouveau).
                  </li>
                  <li>
                    <strong>Tokeniser une carte :</strong> dans l'onglet Tokenisation,
                    sélectionnez une carte et un wallet provider (Apple Pay, Google Pay…).
                    Cliquez sur « Tokenize ». La liste des tokens apparaît en dessous du
                    formulaire.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts et cycle de vie</h3>

                <p className="font-semibold mb-1">Carte</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">En attente d'activation</span></td><td className="p-2 border border-gray-700">Carte émise mais pas encore activée par le porteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Active</span></td><td className="p-2 border border-gray-700">Carte opérationnelle, peut être utilisée pour les transactions.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Bloquée</span></td><td className="p-2 border border-gray-700">Carte bloquée, aucune transaction autorisée.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Volée</span></td><td className="p-2 border border-gray-700">Carte déclarée volée, définitivement bloquée.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Perdue</span></td><td className="p-2 border border-gray-700">Carte déclarée perdue, définitivement bloquée.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Renouvelée</span></td><td className="p-2 border border-gray-700">Carte remplacée par une nouvelle. L'ancienne n'est plus valide.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Expirée</span></td><td className="p-2 border border-gray-700">Date d'expiration dépassée. Carte inutilisable.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Clôturée</span></td><td className="p-2 border border-gray-700">Carte fermée définitivement.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Compte</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Compte opérationnel, peut recevoir des crédits et débits.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Inactif</span></td><td className="p-2 border border-gray-700">Compte désactivé, plus de mouvements possibles.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Bloqué</span></td><td className="p-2 border border-gray-700">Compte bloqué, généralement pour cause de suspicion de fraude.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Clôturé</span></td><td className="p-2 border border-gray-700">Compte fermé définitivement.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Token</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Token utilisable pour les paiements mobiles.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Suspendu</span></td><td className="p-2 border border-gray-700">Token suspendu, temporairement inutilisable.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Révoqué</span></td><td className="p-2 border border-gray-700">Token révoqué définitivement.</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Rôle de chaque bouton et champ</h3>
                <ul className="space-y-2">
                  <li>
                    <strong>6 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Cardholders</em> — liste et création des porteurs.</li>
                      <li><em>Cards</em> — cartes du porteur sélectionné, avec actions (Activer, Bloquer, etc.).</li>
                      <li><em>PIN</em> — définition, vérification et changement de code secret.</li>
                      <li><em>Tokenisation</em> — gestion des wallets mobiles.</li>
                      <li><em>Accounts</em> — comptes du porteur, crédit/débit/hold/release.</li>
                      <li><em>Notifications</em> — alertes et notifications du porteur.</li>
                    </ul>
                  </li>
                  <li>
                    <strong>« Activer »</strong> : active une carte qui était en attente d'activation.
                    La carte passe au statut « Active ».
                  </li>
                  <li>
                    <strong>« Bloquer »</strong> : bloque une carte. La carte passe au statut
                    « Bloquée » et ne peut plus être utilisée.
                  </li>
                  <li>
                    <strong>« Débloquer »</strong> : débloque une carte précédemment bloquée.
                    La carte repasse en statut « Active ».
                  </li>
                  <li>
                    <strong>« Perdue »</strong> : déclare la carte comme perdue. La carte est
                    définitivement bloquée.
                  </li>
                  <li>
                    <strong>« Volée »</strong> : déclare la carte comme volée. La carte est
                    définitivement bloquée.
                  </li>
                  <li>
                    <strong>« Renouveler »</strong> : remplace la carte par une nouvelle
                    (nouveau numéro, nouvelle date d'expiration). L'ancienne est marquée
                    « Renouvelée ».
                  </li>
                  <li>
                    <strong>« Credit »</strong> : crédite le compte du montant saisi.
                  </li>
                  <li>
                    <strong>« Debit »</strong> : débite le compte du montant saisi.
                  </li>
                  <li>
                    <strong>« Place Hold »</strong> : bloque temporairement un montant sur
                    le compte (réservation de fonds).
                  </li>
                  <li>
                    <strong>« Release Hold »</strong> : libère la réservation de fonds,
                    le montant redevient disponible.
                  </li>
                  <li>
                    <strong>« Tokenize »</strong> : crée un token (DPAN) pour une carte
                    auprès d'un wallet provider (Apple Pay, Google Pay…).
                  </li>
                  <li>
                    <strong>« Activate Token » / « Revoke Token »</strong> : active ou
                    suspend un token existant.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Quelle est la différence entre le solde et le solde disponible ?</strong></p>
                  <p>
                    Le <strong>solde (balance)</strong> est le montant total sur le compte.
                    Le <strong>solde disponible (available balance)</strong> est le solde moins
                    les réservations (holds) en cours. Exemple : solde 1 000 TND, hold de 100 TND
                    → solde disponible = 900 TND.
                  </p>

                  <p><strong>Un hold est-il définitif ?</strong></p>
                  <p>
                    Non. Un hold est temporaire. Il peut être libéré (Release Hold) depuis cette
                    page pour annuler la réservation. La capture (conversion en débit réel) se
                    fait via le flux d'autorisation, pas depuis l'interface Issuing. Si un hold
                    n'est ni libéré ni capturé, il expire automatiquement au bout de 30 minutes
                    et le montant est débloqué.
                  </p>

                  <p><strong>Peut-on avoir plusieurs cartes sur le même compte ?</strong></p>
                  <p>
                    Oui. Un porteur peut avoir plusieurs cartes liées au même compte ou à des
                    comptes différents. Chaque carte a son propre statut, son propre PIN et son
                    propre plafond.
                  </p>

                  <p><strong>Qu'est-ce que la tokenisation ?</strong></p>
                  <p>
                    La tokenisation remplace le vrai numéro de carte (PAN) par un numéro
                    virtuel (DPAN) utilisé par Apple Pay, Google Pay, etc. Si le token est
                    compromis, il peut être suspendu sans impacter la carte physique ni son
                    numéro réel.
                  </p>

                  <p><strong>Peut-on définir un PIN après émission ?</strong></p>
                  <p>
                    Oui. Dans l'onglet PIN, sélectionnez la carte, saisissez le PIN en clair
                    et un bloc PIN (pin block), puis cliquez sur « Set PIN ». Le PIN peut aussi
                    être vérifié ou changé ultérieurement.
                  </p>
                </div>
              </section>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
