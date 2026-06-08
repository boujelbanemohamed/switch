import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export const SETTLEMENT_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  SETTLED: 'Réglé',
};

export function NettingHelp() {
  const { t } = useTranslation();
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
              <h2 className="text-lg font-bold">Aide — Netting Multilatéral</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page est le moteur de <strong>compensation multilatérale (netting)</strong> : elle calcule ce
                  que chaque participant doit payer ou recevoir après avoir agré gé toutes ses obligations de la
                  journée. Au lieu de régler chaque transaction une par une, on agré ge les montants pour chaque
                  participant et on ne règle que la position nette.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Netting (compensation multilatérale) :</strong> technique qui consiste à calculer une
                    position unique pour chaque participant en agrégeant tous ses débits et crédits. Si le
                    participant A doit 1 000 TND au total et qu'on lui doit 600 TND, il ne règle que 400 TND.
                  </li>
                  <li>
                    <strong>Position nette :</strong> résultat du calcul pour un participant. Si les crédits
                    dépassent les débits, la position est <em>créditrice</em> (le participant reçoit de l'argent).
                    Sinon, elle est <em>débitrice</em> (il doit payer).
                  </li>
                  <li>
                    <strong>Efficacité du netting :</strong> pourcentage qui mesure la réduction du montant
                    brut total grâce au netting. Plus il est élevé, plus le netting est efficace pour réduire
                    les flux de règlement.
                  </li>
                  <li>
                    <strong>Règlement (settlement) :</strong> étape finale où les positions nettes sont
                    effectivement réglées entre les participants.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Cliquez sur « Calculer »</strong> pour générer une nouvelle session de netting.
                    Le système agré ge automatiquement les transactions de la journée et calcule les positions
                    nettes de chaque participant.
                  </li>
                  <li>
                    <strong>Consultez les cartes résumé</strong> en haut de la page : la date de session,
                    le montant brut total (somme de toutes les obligations), le montant net total (somme
                    des positions nettes) et l'efficacité du netting.
                  </li>
                  <li>
                    <strong>Examinez le tableau des positions</strong> : chaque ligne correspond à un
                    participant. Vérifiez son crédit brut, son débit brut, sa position nette, le type
                    de position (crédit/débit) et son statut de règlement.
                  </li>
                  <li>
                    Le statut de règlement passe de <em>En attente (PENDING)</em> à <em>Réglé (SETTLED)</em>
                    une fois la session confirmée et réglée. Une fois réglée, la session est clôturée
                    et n'apparaît plus comme dernière session active — créez-en une nouvelle pour le
                    prochain cycle de netting.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  4. Statuts de règlement
                </h3>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td className="p-2 border border-gray-700">
                        <span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">En attente (PENDING)</span>
                      </td>
                      <td className="p-2 border border-gray-700">
                        La position a été calculée mais pas encore réglée. Aucun transfert de fonds n'a eu lieu.
                      </td>
                    </tr>
                    <tr>
                      <td className="p-2 border border-gray-700">
                        <span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Réglé (SETTLED)</span>
                      </td>
                      <td className="p-2 border border-gray-700">
                        La position a été réglée. Le participant a payé ou reçu le montant net.
                      </td>
                    </tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  5. Rôle de chaque bouton et champ
                </h3>
                <ul className="space-y-2">
                  <li>
                    <strong>Bouton « Calculer »</strong> : lance le calcul d'une nouvelle session de netting
                    en agrégeant toutes les transactions du jour. Remplace la session existante.
                  </li>
                  <li>
                    <strong>Bouton d'actualisation</strong> (icône de rafraîchissement) : recharge la dernière
                    session sans recalculer.
                  </li>
                  <li>
                    <strong>4 cartes résumé</strong> affichent les indicateurs clés de la session :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Date session</em> : la date de la session de netting.</li>
                      <li><em>Brut total</em> : somme de tous les montants bruts (débits + crédits).</li>
                      <li><em>Net total</em> : somme des positions nettes après compensation.</li>
                      <li><em>Efficacité</em> : pourcentage de réduction du brut grâce au netting.</li>
                    </ul>
                  </li>
                  <li>
                    <strong>Tableau des positions</strong> : liste les participants avec leur crédit brut,
                    débit brut, position nette, type (icône verte = créditeur, rouge = débiteur) et
                    statut de règlement.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Quelle est la différence entre le clearing et le netting ?</strong></p>
                  <p>
                    Le <em>clearing</em> (compensation) est l'étape où les transactions sont distribuées aux
                    différents participants et rapprochées. Le <em>netting</em> vient après : il calcule les
                    positions nettes de chaque participant pour réduire le nombre de règlements. En pratique,
                    le clearing prépare les données, le netting les synthétise en positions nettes.
                  </p>

                  <p><strong>Que faire si une session de netting est vide ?</strong></p>
                  <p>
                    Cliquez sur « Calculer ». Si aucune transaction n'est disponible pour la date du jour,
                    le système affichera un message indiquant qu'aucune session n'est disponible. Assurez-vous
                    que des transactions ont été compensées au préalable (page Clearing).
                  </p>

                  <p><strong>Comment lire l'efficacité du netting ?</strong></p>
                  <p>
                    Une efficacité de 100 % signifie que toutes les obligations se sont annulées et qu'aucun
                    règlement n'est nécessaire (cas idéal mais rare). Plus le pourcentage est élevé, plus
                    le netting réduit les flux financiers.
                  </p>

                  <p><strong>Un participant peut-il avoir une position négative ?</strong></p>
                  <p>
                    Oui, cela signifie qu'il doit de l'argent (position débitrice, icône rouge). Une position
                    positive signifie qu'il reçoit de l'argent (position créditrice, icône verte).
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
