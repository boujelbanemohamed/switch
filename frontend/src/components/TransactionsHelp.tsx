import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const TRANSACTION_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  ROUTING: 'Routage',
  PROCESSING: 'Traitement',
  COMPLETED: 'Complétée',
  FAILED: 'Échouée',
  TIMEOUT: 'Délai expiré',
  REJECTED: 'Rejetée',
};

export const TRANSACTION_TYPE_LABELS: Record<string, string> = {
  PURC: 'Achat',
  PRAU: 'Pré-autorisation',
  COMP: 'Complétion',
  REFD: 'Remboursement',
  VOID: 'Annulation',
  REVS: 'Rejet / Reversal',
  OTHR: 'Autre',
};

export const CHANNEL_LABELS: Record<string, string> = {
  POS: 'Point de Vente',
  ATM: 'Distributeur',
  ECOM: 'E-Commerce',
};

export const TRANSACTION_STATUS_COLORS: Record<string, string> = {
  PENDING: '#eab308',
  ROUTING: '#3b82f6',
  PROCESSING: '#8b5cf6',
  COMPLETED: '#22c55e',
  FAILED: '#ef4444',
  TIMEOUT: '#f97316',
  REJECTED: '#dc2626',
};

export const CHANNEL_COLORS: Record<string, string> = {
  POS: '#3b82f6',
  ATM: '#8b5cf6',
  ECOM: '#f59e0b',
};

export function TransactionsHelp() {
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
              <h2 className="text-lg font-bold">Aide — Transactions</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page affiche l'ensemble des transactions qui transitent par le switch.
                  Chaque transaction représente une opération financière (paiement, retrait,
                  remboursement, etc.) entre un porteur de carte et un commerçant, routée via
                  la plateforme.
                </p>
                <p className="mt-2">
                  Vous pouvez&nbsp;:
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter l'historique des transactions</li>
                  <li>Filtrer par canal (POS, ATM, E-Commerce) et par type d'opération</li>
                  <li>Suivre le statut et le temps de traitement de chaque transaction</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Transaction ID</strong> — identifiant unique de la transaction dans le switch (généré par le routeur).</li>
                  <li><strong>Message Type</strong> — code ISO 8583 indiquant la nature du message (0100 = autorisation, 0200 = transaction financière, 0220 = complétion, etc.).</li>
                  <li><strong>Protocole</strong> — version du protocole utilisée (ISO 8583:1987, ISO 8583:2003, ou ISO 20022).</li>
                  <li><strong>Canal (Channel)</strong> — point d'entrée de la transaction&nbsp;: POS (terminal de paiement), ATM (distributeur), ECOM (paiement en ligne).</li>
                  <li><strong>Type d'opération</strong> — nature financière dérivée du message&nbsp;: achat, pré-autorisation, complétion, remboursement, annulation, rejet.</li>
                  <li><strong>Code réponse</strong> — code retourné par l'émetteur ou le switch (00 = approuvé, 05 = refusé, etc.).</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Filtres</h3>
                <div className="space-y-2">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Par canal</h4>
                    <ul className="list-disc list-inside text-gray-300 ml-2 space-y-1">
                      <li><strong>POS</strong> — transactions depuis un terminal de paiement physique</li>
                      <li><strong>ATM</strong> — transactions depuis un distributeur automatique</li>
                      <li><strong>E-Commerce</strong> — transactions en ligne (via 3-D Secure)</li>
                    </ul>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Par type d'opération</h4>
                    <ul className="list-disc list-inside text-gray-300 ml-2 space-y-1">
                      <li><strong>Achat (PURC)</strong> — transaction d'achat standard 0200</li>
                      <li><strong>Pré-autorisation (PRAU)</strong> — blocage temporaire de montant 0100</li>
                      <li><strong>Complétion (COMP)</strong> — confirmation d'une pré-autorisation 0220</li>
                      <li><strong>Remboursement (REFD)</strong> — retour de fonds 0200 avec code 20xxxx</li>
                      <li><strong>Annulation (VOID)</strong> — annulation avant completion 21xxxx</li>
                      <li><strong>Rejet (REVS)</strong> — reversal technique 0400/0420</li>
                    </ul>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts</h3>
                <ul className="space-y-1">
                  {Object.entries(TRANSACTION_STATUS_LABELS).map(([k, v]) => (
                    <li key={k}>
                      <span style={{
                        display: 'inline-block',
                        width: 10, height: 10,
                        borderRadius: '50%',
                        background: TRANSACTION_STATUS_COLORS[k],
                        marginRight: 6,
                      }} />
                      <code>{k}</code> — {v}
                    </li>
                  ))}
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement technique</h3>
                <p>
                  Chaque transaction suit un cycle défini par le <code>SwitchCore</code>&nbsp;:
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Réception du message ISO 8583/20022</li>
                  <li>Identification de la carte (BIN → émetteur) via <code>RoutingEngine</code></li>
                  <li>Validation des règles de routage et des plafonds</li>
                  <li>Application du stand-in (STIP) si l'émetteur est injoignable</li>
                  <li>Envoi de la réponse au terminal/acquéreur</li>
                  <li>Enregistrement en base pour suivi et clearing</li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Que signifie une transaction en statut ROUTING ?</dt>
                    <dd className="text-gray-300 mt-1">Le switch est en train de déterminer l'émetteur via la table BIN et d'appliquer les règles de routage. Ce statut est normalement transitoire (quelques millisecondes).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Pourquoi une transaction reste-t-elle en PENDING ?</dt>
                    <dd className="text-gray-300 mt-1">Cela peut indiquer un timeout de l'émetteur ou une indisponibilité réseau. Si le temps de traitement dépasse la normale, le switch déclenche le stand-in ou marque la transaction comme TIMEOUT.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la différence entre FAILED et REJECTED ?</dt>
                    <dd className="text-gray-300 mt-1"><code>FAILED</code> = erreur technique (format invalide, timeout, panne). <code>REJECTED</code> = refus métier (plafond dépassé, carte inactive, règles de routage non satisfaites).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je voir les transactions d'un jour spécifique ?</dt>
                    <dd className="text-gray-300 mt-1">La vue actuelle affiche les dernières transactions. Le filtrage par date peut être ajouté en backend selon les besoins.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Le code réponse 00 garantit-il que la transaction est finalisée ?</dt>
                    <dd className="text-gray-300 mt-1">Non. Le code 00 = approuvé par l'émetteur, mais la transaction n'est définitive qu'après clearing et règlement. Une complétion (0220) est nécessaire pour les pré-autorisations.</dd>
                  </div>
                </dl>
              </section>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
