import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export function FxRatesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Taux de Change / FX</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère les <strong>taux de change (FX)</strong> utilisés par la plateforme
                  pour convertir les montants entre devises. Les taux sont saisis manuellement et
                  servent au calcul des transactions multi-devises et à la proposition de
                  <strong>conversion dynamique de devise (DCC)</strong>.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Taux de change</strong> — valeur d'une unité de la devise source dans la devise cible.
                    Exemple&nbsp;: EUR/TND = 3.40 signifie que 1 EUR vaut 3.40 TND.</li>
                  <li><strong>Marge (marginPercentage)</strong> — pourcentage ajouté au taux interbancaire
                    pour couvrir le risque de change et les frais de conversion. Affiché séparément du taux.</li>
                  <li><strong>Paire de devises</strong> — le format <code>source/target</code> (ex: EUR/TND).
                    Le taux indique combien d'unités de la devise cible on obtient pour une unité de la devise source.</li>
                  <li><strong>Date d'effet</strong> — date à partir de laquelle le taux s'applique.
                    Les taux programmés sont chargés automatiquement à leur date d'entrée en vigueur.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Ajouter un taux</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Ajouter</strong></li>
                      <li>Saisir la devise source (ex: EUR) et la devise cible (ex: TND)</li>
                      <li>Saisir le taux de change</li>
                      <li>Choisir la date d'effet</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Convertir un montant</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Saisir le montant dans le champ <strong>Montant</strong></li>
                      <li>Indiquer la devise source et la devise cible</li>
                      <li>Cliquer sur <strong>Convertir</strong></li>
                      <li>Le résultat affiche le montant converti <strong>et</strong> la proposition DCC (avec marge)</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. DCC — Conversion Dynamique de Devise</h3>
                <p>
                  Le <strong>DCC (Dynamic Currency Conversion)</strong> est un service proposé lors d'un
                  paiement international. Le porteur peut choisir de payer dans sa devise domestique plutôt
                  que dans la devise du commerçant. Le switch calcule alors un montant DCC incluant le taux
                  de change et la marge, et le soumet à l'autorisation.
                </p>
                <p className="mt-2">
                  Dans cette page, la section <strong>Convertisseur</strong> simule ce calcul&nbsp;:
                  le montant converti simple (taux brut) et le montant DCC (taux + marge) sont affichés
                  côte à côte.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement technique</h3>
                <p>
                  Le service <code>FxService</code> cherche le taux applicable via
                  <code> sourceCurrency, targetCurrency et effectiveDate</code>. Le taux
                  le plus récent avant ou égal à la date demandée est utilisé. La conversion
                  applique&nbsp;:
                </p>
                <p className="mt-2 font-mono text-xs bg-gray-800 p-2 rounded">
                  montant converti = montant source × taux<br />
                  montant DCC = montant converti × (1 + margin%)
                </p>
                <p className="mt-2">
                  Les taux sont stockés dans la table <code>fx_rates</code>. Chaque mise à jour
                  conserve l'historique (les anciens taux ne sont pas supprimés).
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Comment le taux est-il appliqué à une transaction ?</dt>
                    <dd className="text-gray-300 mt-1">Le switch utilise le taux FX au moment du routage de la transaction, en fonction des devises source et cible. La conversion est tracée dans les logs de la transaction.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je modifier un taux existant ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, via l'API PUT. La modification met à jour le taux et la date de mise à jour. L'historique n'est pas effacé.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la précision des taux ?</dt>
                    <dd className="text-gray-300 mt-1">Les taux sont stockés avec 8 décimales de précision (échelle 8). Les montants convertis sont arrondis à 3 décimales.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Le DCC est-il obligatoire ?</dt>
                    <dd className="text-gray-300 mt-1">Non. Le DCC est une option proposée au porteur. Sans DCC, la transaction est traitée dans la devise du commerçant avec le taux interbancaire standard.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si je ne saisis pas de marge ?</dt>
                    <dd className="text-gray-300 mt-1">La marge par défaut est 0%. Le taux de change brut est alors identique au taux DCC.</dd>
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
