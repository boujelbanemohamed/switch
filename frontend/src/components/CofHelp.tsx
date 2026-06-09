import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const TOKEN_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  CANCELLED: 'Annulé',
  EXPIRED: 'Expiré',
};

export const SCHEDULE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  PAUSED: 'En pause',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
};

export const FREQUENCY_LABELS: Record<string, string> = {
  DAILY: 'Quotidien',
  WEEKLY: 'Hebdomadaire',
  MONTHLY: 'Mensuel',
  YEARLY: 'Annuel',
};

export const TOKEN_TYPE_LABELS: Record<string, string> = {
  UNSCHEDULED: 'Non planifié',
  SCHEDULED: 'Planifié',
  RECURRING: 'Récurrent',
  INSTALLMENT: 'Échéancier',
};

export function CofHelp() {
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
              <h2 className="text-lg font-bold">Aide — Card on File / COF</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  La fonctionnalité <strong>Card on File (COF)</strong> permet de stocker
                  des références de cartes de paiement pour des transactions ultérieures
                  sans que le porteur ait à ressaisir ses données. Elle est utilisée pour&nbsp;:
                </p>
                <ul className="list-disc list-inside mt-2 space-y-1">
                  <li>Les <strong>paiements récurrents</strong> (abonnements, mensualités)</li>
                  <li>Les <strong>paiements en un clic</strong> (cartes enregistrées chez un marchand)</li>
                  <li>Les <strong>échéanciers</strong> (paiements programmés à dates fixes)</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Token COF</strong> — référence sécurisée qui remplace les données de carte
                    réelles. Seul un affichage partiel (PAN masqué) et une référence interne sont stockés.
                    Pas de numéro complet, pas de cryptogramme.</li>
                  <li><strong>PAN display</strong> — représentation partielle du numéro de carte
                    (ex: <code>4970XXXX</code>). Permet au porteur d'identifier sa carte sans exposer
                    le numéro complet (limité à 8 caractères).</li>
                  <li><strong>PAN reference</strong> — identifiant interne liant ce token au porteur
                    dans le système du commerçant ou de l'émetteur.</li>
                  <li><strong>Échéancier récurrent</strong> — plan de paiement avec une fréquence
                    (quotidienne, hebdomadaire, mensuelle), un montant, et un nombre maximum d'exécutions.</li>
                  <li><strong>Token type</strong> — catégorie d'usage&nbsp;:
                    <code>UNSCHEDULED</code> (usage unique sans planification),
                    <code>SCHEDULED</code> (programmé),
                    <code>RECURRING</code> (récurrent ouvert),
                    <code>INSTALLMENT</code> (échéancier fixe).</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Créer un token COF</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Ajouter</strong> dans la section Tokens</li>
                      <li>Saisir un PAN display (ex: <code>4970XXXX</code>)</li>
                      <li>Saisir un PAN reference (identifiant interne)</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Créer un échéancier</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Ajouter</strong> dans la section Échéanciers</li>
                      <li>Saisir le montant et la devise</li>
                      <li>Choisir la fréquence (quotidien, hebdo, mensuel)</li>
                      <li>Définir la date de prochaine exécution</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong></li>
                    </ol>
                    <p className="text-xs text-gray-400 mt-1">Note&nbsp;: le champ <code>cofTokenId</code> n'est pas affiché dans cette version simplifiée.</p>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts</h3>
                <p className="mb-2">Tokens&nbsp;:</p>
                <ul className="space-y-1 mb-3">
                  {Object.entries(TOKEN_STATUS_LABELS).map(([k, v]) => (
                    <li key={k}><code>{k}</code> — {v}</li>
                  ))}
                </ul>
                <p className="mb-2">Échéanciers&nbsp;:</p>
                <ul className="space-y-1">
                  {Object.entries(SCHEDULE_STATUS_LABELS).map(([k, v]) => (
                    <li key={k}><code>{k}</code> — {v}</li>
                  ))}
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement technique</h3>
                <p>
                  Les tokens COF sont stockés dans la table <code>cof_tokens</code>.
                  Chaque token est lié à un participant (émetteur) et peut avoir plusieurs
                  échéanciers dans la table <code>recurring_schedules</code>.
                </p>
                <p className="mt-2">
                  Le service <code>CofTokenService</code> gère le cycle de vie des tokens
                  (création, suspension, annulation). Les échéanciers sont exécutés automatiquement chaque nuit à 5h
                  par un batch programmé (<code>@Scheduled</code>) qui met à jour la date
                  de prochaine exécution et le compteur d'occurrences.
                </p>
                <p className="mt-2">
                  Chaque exécution d'échéancier vérifie le nombre d'occurrences
                  max et la date de fin avant de mettre à jour l'échéancier.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Un token COF expire-t-il ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Le statut passe à <code>EXPIRED</code> lorsque la carte sous-jacente arrive à expiration (date de fin de validité dépassée).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je suspendre un échéancier sans supprimer le token ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, en modifiant le statut de l'échéancier vers <code>PAUSED</code>. Le token associé reste actif pour d'autres usages.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la différence entre UNSCHEDULED et RECURRING ?</dt>
                    <dd className="text-gray-300 mt-1"><code>UNSCHEDULED</code> = le token existe mais aucun échéancier n'est attaché (paiement en un clic). <code>RECURRING</code> = un ou plusieurs échéanciers sont actifs.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les tokens sont-ils visibles dans les transactions ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Chaque transaction issue d'un échéancier contient une référence au token COF et au schedule dans ses métadonnées.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Le PAN display est-il le vrai numéro de carte ?</dt>
                    <dd className="text-gray-300 mt-1">Non, c'est une version masquée limitée à 8 caractères. Le PAN complet n'est jamais stocké en clair.</dd>
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
