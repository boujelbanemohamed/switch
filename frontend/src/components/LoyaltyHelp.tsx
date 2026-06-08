import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const MEMBERSHIP_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  CANCELLED: 'Annulé',
};

export const TX_TYPE_LABELS: Record<string, string> = {
  EARN: 'Gain',
  BURN: 'Utilisation',
  ADJUST: 'Ajustement',
  EXPIRE: 'Expiré',
};

export const REWARD_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  OUT_OF_STOCK: 'Épuisé',
};

export const REDEMPTION_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  COMPLETED: 'Effectué',
  CANCELLED: 'Annulé',
};

export function LoyaltyHelp() {
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
              <h2 className="text-lg font-bold">Aide — Fidélité</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère les <strong>programmes de fidélité</strong> : création de programmes,
                  niveaux (tiers), adhésions des porteurs, accumulation et utilisation des points,
                  et catalogue de récompenses. Chaque achat rapporté des points qui peuvent être
                  échangés contre des récompenses ou un crédit sur le compte.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Points de fidélité :</strong> unité de valeur accumulée à chaque achat.
                    Calcul : <em>montant × taux d'acquisition (earningRate) × multiplicateur du niveau</em>.
                    Exemple : 50 TND × 1,0 × 1,2 (Silver) = 60 points.
                  </li>
                  <li>
                    <strong>Niveaux (tiers) :</strong> catégories de porteurs (Silver, Gold, Platinum)
                    avec un multiplicateur croissant. Le niveau est déterminé par le total de points
                    accumulés à vie (<em>lifetimePoints</em>). Plus le niveau est élevé, plus chaque
                    achat rapporté de points.
                  </li>
                  <li>
                    <strong>Expiration FIFO :</strong> les points expirent dans l'ordre où ils ont été
                    gagnés — les plus anciens points sont utilisés ou expirent en premier. Une
                    transaction d'utilisation (BURN) consomme d'abord les points les plus vieux.
                    Ce mécanisme évite que des points récents expirent avant des points anciens.
                  </li>
                  <li>
                    <strong>Gain (EARN) :</strong> transaction qui crédite des points sur l'adhésion.
                    Déclenchée par un achat éligible.
                  </li>
                  <li>
                    <strong>Utilisation (BURN) :</strong> transaction qui débite des points de l'adhésion.
                    Les points sont consommés dans l'ordre FIFO pour le calcul d'expiration.
                  </li>
                  <li>
                    <strong>Rachat (redemption) :</strong> échange de points contre une récompense
                    (catalogue) ou un crédit sur le compte balance.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Créer un programme :</strong> dans l'onglet Programmes, cliquez sur
                    « Créer un programme ». Donnez-lui un nom, un taux d'acquisition (earningRate)
                    et une devise.
                  </li>
                  <li>
                    <strong>Ajouter des niveaux :</strong> sélectionnez un programme, cliquez sur
                    « Créer un niveau ». Définissez le nom, le seuil de points à vie requis et le
                    multiplicateur (ex. : Silver=1,2, Gold=1,5, Platinum=2,0).
                  </li>
                  <li>
                    <strong>Activer / désactiver :</strong> utilisez le bouton toggle pour activer
                    ou désactiver un programme. Un programme inactif ne peut plus accumuler de points.
                  </li>
                  <li>
                    <strong>Adhérer un porteur :</strong> dans l'onglet Adhésions, cliquez sur
                    « Adhérer ». Saisissez l'ID du porteur (cardholder). L'adhésion démarre avec
                    solde = 0 et le niveau le plus bas.
                  </li>
                  <li>
                    <strong>Gagner des points :</strong> sélectionnez une adhésion, cliquez sur
                    « Gagner des points ». Saisissez le montant de la transaction. Le système
                    calcule automatiquement les points gagnés (montant × taux × multiplicateur).
                  </li>
                  <li>
                    <strong>Utiliser des points :</strong> sélectionnez une adhésion, cliquez sur
                    « Utiliser des points ». Saisissez le nombre de points à débiter. Les plus
                    anciens points sont consommés en premier (FIFO).
                  </li>
                  <li>
                    <strong>Créer des récompenses :</strong> dans l'onglet Récompenses, cliquez sur
                    « Créer une récompense ». Définissez le nom, le coût en points et le stock
                    disponible.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  4. Statuts et cycle de vie
                </h3>

                <p className="font-semibold mb-1">Programme / Niveau / Récompense</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Le programme/niveau/récompense est opérationnel. Les points peuvent être gagnés ou utilisés.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Inactif</span></td><td className="p-2 border border-gray-700">Désactivé. Aucune opération possible.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Épuisé</span></td><td className="p-2 border border-gray-700">Récompense en rupture de stock. Plus disponible jusqu'au réapprovisionnement.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Adhésion</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Le porteur peut gagner et utiliser des points.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Suspendu</span></td><td className="p-2 border border-gray-700">Adhésion suspendue temporairement. Les points ne sont plus accumulés.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Annulé</span></td><td className="p-2 border border-gray-700">Adhésion résiliée. Les points sont perdus.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Transaction</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Type</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Gain</span></td><td className="p-2 border border-gray-700">Points crédités suite à un achat. Le solde augmente.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Utilisation</span></td><td className="p-2 border border-gray-700">Points débités pour une récompense. Le solde diminue. Les points les plus anciens sont consommés en premier (FIFO).</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Ajustement</span></td><td className="p-2 border border-gray-700">Correction manuelle du solde (opérateur).</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Expiré</span></td><td className="p-2 border border-gray-700">Points arrivés à expiration. Débit automatique.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Rachat (Redemption)</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">En attente</span></td><td className="p-2 border border-gray-700">Demande de rachat créée, en cours de traitement.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Effectué</span></td><td className="p-2 border border-gray-700">Rachat traité. Les points ont été débités et la récompense attribuée.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Annulé</span></td><td className="p-2 border border-gray-700">Rachat annulé. Les points sont restitués.</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  5. Rôle de chaque bouton et champ
                </h3>
                <ul className="space-y-2">
                  <li>
                    <strong>« Créer un programme »</strong> : ouvre une modale pour créer un nouveau
                    programme de fidélité (nom, description, taux d'acquisition, devise).
                  </li>
                  <li>
                    <strong>Toggle Activer/Désactiver</strong> : active ou désactive un programme.
                    Un programme inactif ne peut plus accumuler de points, mais les soldes existants
                    sont conservés.
                  </li>
                  <li>
                    <strong>« Créer un niveau »</strong> : ajoute un niveau (tier) au programme
                    sélectionné (nom, seuil de points, multiplicateur, avantages).
                  </li>
                  <li>
                    <strong>« Adhérer »</strong> : inscrit un porteur au programme sélectionné.
                    L'ID du porteur est requis.
                  </li>
                  <li>
                    <strong>« Gagner des points »</strong> : crédite des points sur une adhésion.
                    Montant × earningRate × tierMultiplier = points gagnés.
                  </li>
                  <li>
                    <strong>« Utiliser des points »</strong> : débite des points d'une adhésion.
                    Les plus anciens points sont consommés en premier (FIFO).
                  </li>
                  <li>
                    <strong>« Suspendre »</strong> : suspend temporairement une adhésion active.
                  </li>
                  <li>
                    <strong>« Créer une récompense »</strong> : ajoute une récompense au catalogue
                    (nom, description, coût en points, stock).
                  </li>
                  <li>
                    <strong>3 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Programmes</em> — liste des programmes avec niveaux imbriqués.</li>
                      <li><em>Adhésions</em> — porteurs inscrits, soldes de points, historique.</li>
                      <li><em>Récompenses</em> — catalogue de récompenses disponibles.</li>
                    </ul>
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Comment sont calculés les points gagnés ?</strong></p>
                  <p>
                    Points = <em>montant × earningRate × tierMultiplier</em>.
                    Exemple : un achat de 50 TND sur un programme avec taux 1,0 et un porteur
                    Silver (multiplicateur 1,2) rapporte 50 × 1,0 × 1,2 = 60 points.
                  </p>

                  <p><strong>Qu'est-ce que l'expiration FIFO ?</strong></p>
                  <p>
                    FIFO (First In, First Out) signifie que les points les plus anciens sont utilisés
                    ou expirent en premier. Si vous avez gagné 100 points en janvier et 100 points en
                    février, une utilisation de 50 points consommera les points de janvier. Ce mécanisme
                    garantit que les points récents ne sont pas perdus avant les points anciens.
                  </p>

                  <p><strong>Quelle est la différence entre EARN et BURN ?</strong></p>
                  <p>
                    <em>EARN</em> (Gain) augmente le solde de points suite à un achat. <em>BURN</em>
                    (Utilisation) diminue le solde lorsqu'un porteur échange ses points contre une
                    récompense ou un crédit.
                  </p>

                  <p><strong>Comment fonctionne le changement de niveau ?</strong></p>
                  <p>
                    Lorsque les points à vie (<em>lifetimePoints</em>) d'un porteur atteignent le seuil
                    d'un niveau supérieur, l'adhésion est automatiquement promue. Le nouveau
                    multiplicateur s'applique à tous les achats futurs.
                  </p>

                  <p><strong>Que se passe-t-il si une récompense est épuisée ?</strong></p>
                  <p>
                    Le statut passe à « Épuisé » (OUT_OF_STOCK). La récompense n'est plus disponible
                    jusqu'à ce que le stock soit réapprovisionné par un opérateur.
                  </p>

                  <p><strong>Puis-je annuler un rachat ?</strong></p>
                  <p>
                    Oui, un rachat peut être annulé. Les points sont alors restitués sur l'adhésion
                    et le statut passe à « Annulé ».
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
