import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const REGION_LABELS: Record<string, string> = {
  TN: 'Tunisie',
  INTL: 'International',
  ALL: 'Tous',
};

export function InterchangeFeesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Frais d'interchange</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. A quoi sert cette page</h3>
                <p>
                  Les <strong>frais d'interchange</strong> sont les commissions que l'acquéreur
                  (la banque du marchand) verse à l'émetteur (la banque du porteur) pour chaque
                  transaction par carte. Ces frais sont définis par les réseaux de cartes
                  (Visa, Mastercard) et varient selon plusieurs critères.
                </p>
                <p className="mt-2">
                  Cette page permet de gerer le <strong>tableau de correspondance</strong> des
                  frais d'interchange : pour chaque combinaison de réseau, type de carte,
                  région et code marchand, on definit un montant fixe et un pourcentage.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter la liste complete des frais d'interchange configures</li>
                  <li>Ajouter, modifier et supprimer des entrees</li>
                  <li>Les frais sont automatiquement appliques lors de la compensation (clearing)</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Frais d'interchange</strong> — la commission interbancaire payee par
                    l'acquéreur à l'émetteur. C'est le cout de base du traitement d'une
                    transaction par carte. Les montants sont reglementes par les reseaux de cartes.</li>
                  <li><strong>Reseau</strong> — la marque de la carte (Visa, Mastercard). Chaque
                    reseau a ses propres grilles tarifaires.</li>
                  <li><strong>Type de carte</strong> — Debit ou Credit (parfois Prepaye). Les
                    frais d'interchange sont généralement plus élevés pour le credit que pour le debit.</li>
                  <li><strong>Region</strong> — le perimetre geographique : <strong>TN</strong> pour
                    les transactions domestiques (Tunisie), <strong>INTL</strong> pour les
                    transactions internationales. Les frais internationaux sont plus élevés.</li>
                  <li><strong>MCC</strong> — le code marchand a 4 chiffres. Ce filtre permet
                    d'avoir des frais differencies par secteur d'activite (ex: 5812 pour la
                    restauration). La valeur <code>*</code> signifie « tous les MCC ».</li>
                  <li><strong>Montant fixe</strong> — frais preleves independamment du montant
                    de la transaction (ex: 0,50 TND par transaction).</li>
                  <li><strong>Pourcentage</strong> — un pourcentage du montant de la transaction
                    (ex: 0,8% du montant).</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Ajouter un frais d'interchange</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouveau frais</strong> (bouton bleu en haut a droite)</li>
                      <li>Saisir le <strong>Reseau</strong> (ex: VISA, MASTERCARD)</li>
                      <li>Saisir le <strong>Type de carte</strong> (ex: DEBIT, CREDIT)</li>
                      <li>Saisir la <strong>Region</strong> (TN pour domestique, INTL pour international)</li>
                      <li>Saisir le <strong>MCC</strong> (4 chiffres, ou <code>*</code> pour tous les codes marchands)</li>
                      <li>Saisir le <strong>Montant fixe</strong> (ex: 0.50)</li>
                      <li>Saisir le <strong>Pourcentage</strong> (ex: 0.8 pour 0,8%)</li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier un frais</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone crayon de la ligne a modifier</li>
                      <li>Les champs deviennent editables directement dans le tableau</li>
                      <li>Modifier les valeurs souhaitees</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong> (vert) pour valider, ou <strong>Annuler</strong> pour abandonner</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer un frais</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone poubelle de la ligne a supprimer</li>
                      <li>Confirmer la suppression dans la boite de dialogue</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>
                <h4 className="font-medium mt-1 mb-1">Valeurs par defaut (initialisees au demarrage)</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Reseau</th>
                      <th className="p-1 text-left">Type</th>
                      <th className="p-1 text-left">Region</th>
                      <th className="p-1 text-right">Fixe</th>
                      <th className="p-1 text-right">%</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">VISA</td><td className="p-1">Debit</td><td className="p-1">TN</td><td className="p-1 text-right">0,50</td><td className="p-1 text-right">0,8%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">VISA</td><td className="p-1">Credit</td><td className="p-1">TN</td><td className="p-1 text-right">0,75</td><td className="p-1 text-right">1,0%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MASTERCARD</td><td className="p-1">Debit</td><td className="p-1">TN</td><td className="p-1 text-right">0,45</td><td className="p-1 text-right">0,75%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MASTERCARD</td><td className="p-1">Credit</td><td className="p-1">TN</td><td className="p-1 text-right">0,70</td><td className="p-1 text-right">0,95%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">VISA</td><td className="p-1">Debit</td><td className="p-1">INTL</td><td className="p-1 text-right">0</td><td className="p-1 text-right">1,2%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">VISA</td><td className="p-1">Credit</td><td className="p-1">INTL</td><td className="p-1 text-right">0</td><td className="p-1 text-right">1,5%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MASTERCARD</td><td className="p-1">Debit</td><td className="p-1">INTL</td><td className="p-1 text-right">0</td><td className="p-1 text-right">1,1%</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MASTERCARD</td><td className="p-1">Credit</td><td className="p-1">INTL</td><td className="p-1 text-right">0</td><td className="p-1 text-right">1,4%</td></tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Resolution par cascade</h4>
                <p className="text-gray-300">
                  Lors de la compensation, le systeme cherche le taux le plus specifique en
                  suivant cet ordre, du plus precis au moins precis :
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Correspondance exacte (reseau + type + region + MCC)</li>
                  <li>Reseau + type + region, MCC = <code>*</code></li>
                  <li>Reseau + type, region = <code>*</code>, MCC = <code>*</code></li>
                  <li>Reseau, type = <code>*</code>, region = <code>*</code>, MCC = <code>*</code></li>
                  <li>Tous <code>*</code> — valeur par defaut (si configuree)</li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Les frais d'interchange sont calcules automatiquement lors de la compensation
                  (clearing) de chaque transaction.
                </p>
                <p className="mt-2">
                  <strong>Formule de calcul :</strong>
                </p>
                <p className="text-gray-300 ml-2 mt-1">
                  frais = montant fixe + (montant de la transaction x pourcentage / 100)
                </p>

                <h4 className="font-medium mt-3 mb-1">Exemples</h4>
                <ul className="space-y-2">
                  <li className="text-gray-300">
                    <strong>VISA Debit TN, 100 TND :</strong><br />
                    frais = 0,50 + (100 x 0,8 / 100) = 0,50 + 0,80 = <strong>1,30 TND</strong>
                  </li>
                  <li className="text-gray-300">
                    <strong>VISA Credit TN, 200 TND :</strong><br />
                    frais = 0,75 + (200 x 1,0 / 100) = 0,75 + 2,00 = <strong>2,75 TND</strong>
                  </li>
                  <li className="text-gray-300">
                    <strong>Mastercard Credit INTL, 500 TND :</strong><br />
                    frais = 0 + (500 x 1,4 / 100) = 0 + 7,00 = <strong>7,00 TND</strong>
                  </li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Lien avec les Barmes de frais</h4>
                <p className="text-gray-300">
                  Cette table d'interchange est <strong>independante</strong> du moteur de barmes
                  de frais (page <strong>Barmes de frais</strong>). Meme si la methode
                  « Recherche interchange » existe dans les barmes, elle n'est pas encore
                  connectee a cette table. Les frais d'interchange sont actuellement calcules
                  directement par le moteur de compensation, pas via les barmes.
                </p>
                <p className="text-gray-300 mt-1">
                  Concretement, cela signifie qu'il existe <strong>deux endroits</strong> ou des
                  frais lies a l'interchange peuvent etre definis :
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Cette page (<strong>Frais d'interchange</strong>) — utilisee directement par la compensation. Les montants sont deduits du montant de la transaction lors du clearing.</li>
                  <li>La page <strong>Barmes de frais</strong> avec la methode « Recherche interchange » — prevue pour une integration future, mais actuellement inactive (retourne 0).</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Les frais d'interchange sont-ils appliques automatiquement aux transactions ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, lors de la compensation (clearing). Le moteur de clearing appelle automatiquement le service de calcul d'interchange avec le reseau, le type de carte, la region, le MCC et le montant de la transaction. Le montant d'interchange est deduit du montant de la transaction pour calculer le montant net.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quels sont les frais d'interchange par defaut au demarrage ?</dt>
                    <dd className="text-gray-300 mt-1">Huit entrees sont initialisees automatiquement : VISA et Mastercard en Debit et Credit, pour les regions TN et INTL (voir le tableau dans la section 4). Les frais TN sont plus faibles (fixe + petit pourcentage) tandis que les frais INTL sont uniquement en pourcentage.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre cette page et les barmes de frais ?</dt>
                    <dd className="text-gray-300 mt-1">Cette page gere la grille d'interchange brute telle que definie par les reseaux (Visa, Mastercard). Elle est directement utilisee par la compensation. La page « Barmes de frais » est un moteur de calcul generique qui peut combiner plusieurs types de frais (interchange, traitement, reseau, etc.) mais n'est pas encore integre au flux de compensation. Les deux systemes coexistent et sont independants.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je utiliser un MCC specifique pour differents secteurs ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Vous pouvez creer autant d'entrees que necessaire avec des MCC differents. Le systeme cherchera d'abord une correspondance exacte sur le MCC avant de tomber sur la valeur par defaut (<code>*</code>).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si aucune correspondance n'est trouvee ?</dt>
                    <dd className="text-gray-300 mt-1">Le systeme retourne 0 (aucun frais d'interchange applique) et emet un avertissement dans les logs. Il est recommande d'avoir au moins une entree avec tous les champs a <code>*</code> comme valeur de secours.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les modifications prennent-elles effet immediatement ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Les frais d'interchange sont lus a chaque calcul de compensation. Une modification, un ajout ou une suppression est pris en compte des la prochaine execution de la compensation.</dd>
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
