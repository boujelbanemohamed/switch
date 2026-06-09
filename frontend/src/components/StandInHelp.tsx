import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const STANDIN_DECISION_LABELS: Record<string, string> = {
  APPROVED: 'Approuvé',
  DECLINED: 'Refusé',
};

export function StandInHelp() {
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
              <h2 className="text-lg font-bold">Aide — Stand-In / STIP</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Le <strong>Stand-In (STIP)</strong> est un mécanisme de secours activé lorsque
                  la banque émettrice est injoignable (panne réseau, timeout, maintenance).
                  Plutôt que de refuser systématiquement les transactions, le switch peut
                  autoriser à la place de l'émetteur selon des règles prédéfinies.
                </p>
                <p className="mt-2">
                  Cette page permet de&nbsp;:
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter et gérer les <strong>règles de stand-in</strong> (création, modification, suppression)</li>
                  <li>Visualiser l'<strong>historique des autorisations</strong> accordées en mode dégradé</li>
                  <li>Suivre le <strong>nombre d'autorisations en attente de réconciliation</strong></li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Règle de stand-in</strong> — définit les conditions dans lesquelles le switch peut autoriser :
                    montant max, nombre d'opérations par jour, plafond journalier, codes MCC autorisés.
                    Une règle peut être spécifique à un émetteur (issuerParticipantId renseigné) ou globale
                    (issuerParticipantId = vide, applicable à tous).</li>
                  <li><strong>Card Brand</strong> — réseau de la carte (VISA, Mastercard, etc.). La valeur spéciale
                    <code> ALL</code> signifie « toutes les marques ».</li>
                  <li><strong>Réconciliation</strong> — lorsque l'émetteur redevient joignable, la
                    réconciliation consiste à transmettre les autorisations STIP pour régularisation.
                    Les autorisations non réconciliées sont signalées en orange.</li>
                  <li><strong>Decline if no rule</strong> — si aucun émetteur ni règle globale ne correspond,
                    la transaction est refusée (comportement par défaut).</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Créer une règle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouvelle règle</strong></li>
                      <li>Laisser <em>Issuer</em> vide pour une règle globale, ou saisir l'UUID d'un participant émetteur</li>
                      <li>Paramétrer les limites (montant max, plafond journalier, etc.)</li>
                      <li>Cocher <em>Enabled</em> pour activer la règle immédiatement</li>
                      <li>Valider avec <strong>Créer</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier une règle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icône crayon de la règle ciblée</li>
                      <li>Modifier les champs directement dans la ligne</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong> (vert) pour valider</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer une règle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icône poubelle de la règle ciblée</li>
                      <li>Confirmer la suppression</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts</h3>
                <ul className="space-y-1">
                  <li><span className="bg-green-900 text-green-300 px-2 py-0.5 rounded text-xs font-medium">Approuvé</span> — autorisation accordée par le stand-in</li>
                  <li><span className="bg-red-900 text-red-300 px-2 py-0.5 rounded text-xs font-medium">Refusé</span> — autorisation refusée par le stand-in (dépassement de plafond, règle désactivée, etc.)</li>
                </ul>
                <h4 className="font-medium mt-3 mb-1">Raisons de refus</h4>
                <ul className="space-y-1 text-gray-300">
                  <li><code>NO_RULE</code> — Aucune règle trouvée pour cet émetteur/marque</li>
                  <li><code>RULE_DISABLED</code> — La règle existe mais est désactivée</li>
                  <li><code>EXCEEDS_MAX_AMOUNT</code> — Montant de la transaction &gt; plafond autorisé</li>
                  <li><code>MCC_NOT_ALLOWED</code> — Code marchand non autorisé par la règle</li>
                  <li><code>DAILY_COUNT_LIMIT</code> — Nombre d'opérations du jour dépassé</li>
                  <li><code>DAILY_AMOUNT_LIMIT</code> — Plafond journalier de montant dépassé</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement technique</h3>
                <p>
                  Lorsqu'une transaction arrive dans le switch et que l'émetteur ne répond pas
                  (timeout ou erreur réseau), le <code>SwitchCore</code> appelle
                  <code> StandInService.attemptStandIn()</code> avec les paramètres de la transaction.
                </p>
                <p className="mt-2">
                  L'algorithme cherche une règle applicable&nbsp;:
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Règle spécifique (émetteur + marche de carte exacte)</li>
                  <li>Règle spécifique (émetteur + <code>ALL</code>)</li>
                  <li>Règle globale (<code>issuerParticipantId = null</code>)</li>
                </ol>
                <p className="mt-2">
                  Si une règle est trouvée et que tous les contrôles passent (montant, plafond,
                  MCC), la transaction est <strong>APPROUVÉE</strong> en stand-in.
                  Sinon, elle est <strong>REFUSÉE</strong> avec un motif.
                  Toutes les décisions sont enregistrées dans la table <code>stand_in_authorizations</code>
                  pour réconciliation ultérieure.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quand le stand-in est-il déclenché ?</dt>
                    <dd className="text-gray-300 mt-1">Automatiquement par le switch quand l'émetteur est injoignable après timeout. Un événement <code>StandInUsedEvent</code> est publié pour traçabilité.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les autorisations STIP sont-elles définitives ?</dt>
                    <dd className="text-gray-300 mt-1">Non. L'émetteur doit les réconcilier une fois revenu en ligne. Tant qu'elles ne sont pas réconciliées (<code>reconciled = false</code>), elles apparaissent en attente.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si aucune règle n'existe ?</dt>
                    <dd className="text-gray-300 mt-1">Avec <em>declineIfNoRule = true</em> (valeur par défaut), la transaction est refusée avec le motif <code>NO_RULE</code>. Passez à <em>false</em> uniquement si vous avez une règle globale de fallback.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je limiter les STIP à certains types de commerçants ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, via le champ <em>Allowed MCC</em>. Saisissez une liste séparée par des virgules (ex: <code>5812,5813</code>) ou <code>*</code> pour tous.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Comment savoir si une transaction a été traitée en STIP ?</dt>
                    <dd className="text-gray-300 mt-1">Le champ <code>standInUsed</code> du modèle <code>Transaction</code> passe à <code>true</code>. Consultez aussi le tableau des autorisations sur cette page.</dd>
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
