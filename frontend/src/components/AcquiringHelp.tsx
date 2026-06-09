import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const MERCHANT_STATUS_LABELS: Record<string, string> = {
  PENDING_ONBOARDING: 'En attente',
  PENDING_APPROVAL: 'En attente',
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  TERMINATED: 'Résilié',
  UNDER_REVIEW: 'En révision',
};

export const TERMINAL_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  SUSPENDED: 'Suspendu',
  RETIRED: 'Retiré',
  MALFUNCTION: 'Défaillant',
};

export const SETTLEMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  CONFIRMED: 'Confirmé',
  PAID: 'Payé',
  DISPUTED: 'Contesté',
  CANCELLED: 'Annulé',
};

export function AcquiringHelp() {
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
              <h2 className="text-lg font-bold">Aide — Acquiring</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère l'<strong>acquisition</strong> — les commerçants, leurs
                  terminaux de paiement et les règlements associés. Elle permet d'ajouter
                  des commerçants, d'enregistrer des terminaux POS, d'injecter des clés
                  de sécurité, de créer des règlements et de visualiser le netting.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Commerçant (Merchant) :</strong> entité commerciale qui accepte
                    les paiements par carte. Chaque commerçant a un code MCC (Merchant
                    Category Code), un pays et un statut (Actif, Suspendu, Résilié…).
                  </li>
                  <li>
                    <strong>Terminal :</strong> dispositif POS physique ou logiciel lié à
                    un commerçant. Chaque terminal a un identifiant, un modèle, un numéro
                    de série et des clés de sécurité (MKey, PIK, MAK).
                  </li>
                  <li>
                    <strong>Clés de sécurité :</strong> MKey (Master Key), PIK (PIN
                    Encryption Key), MAK (MAC Authentication Key). Elles sont injectées
                    dans le terminal pour sécuriser les transactions.
                  </li>
                  <li>
                    <strong>Règlement (Settlement) :</strong> transfert de fonds entre
                    l'acquéreur et le commerçant pour les transactions effectuées sur une
                    période donnée. Le règlement passe par les états : En attente →
                    Confirmé → Payé.
                  </li>
                  <li>
                    <strong>Netting :</strong> calcul de la position nette (débits bruts
                    moins crédits bruts) pour une période. Résultat : montant net dû au
                    commerçant ou à l'acquéreur.
                  </li>
                  <li>
                    <strong>Banque acquéreuse (Acquiring Bank) :</strong> institution
                    financière qui acquiert les transactions pour le compte du commerçant
                    et assure le règlement. Sélectionnée lors de la création du commerçant.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Ajouter un commerçant :</strong> dans l'onglet Merchants,
                    cliquez sur « Create Merchant ». Remplissez le nom, le MCC, le pays, les
                    coordonnées et sélectionnez la banque acquéreuse. Le code commerçant est
                    généré automatiquement à partir du nom.
                  </li>
                  <li>
                    <strong>Sélectionner un commerçant :</strong> cliquez sur une ligne dans
                    le tableau Merchants pour voir ses terminaux.
                  </li>
                  <li>
                    <strong>Enregistrer un terminal :</strong> dans l'onglet Terminals,
                    sélectionnez un commerçant, cliquez sur « Register Terminal ». Saisissez
                    l'ID terminal, le numéro de série, le modèle et l'emplacement.
                  </li>
                  <li>
                    <strong>Injecter des clés :</strong> dans l'onglet Terminals, utilisez
                    le formulaire en bas de page ou cliquez sur « Inject Keys » dans les
                    actions d'un terminal. Remplissez MKey, PIK et MAK.
                  </li>
                  <li>
                    <strong>Créer un règlement :</strong> dans l'onglet Settlement,
                    sélectionnez un commerçant, une période et une date de règlement.
                    Cliquez sur « Create Settlement ».
                  </li>
                  <li>
                    <strong>Consulter les règlements :</strong> dans l'onglet Settlement,
                    sélectionnez un commerçant et une période, puis cliquez sur « Load ».
                    Les résultats de netting apparaissent sous le tableau.
                  </li>
                  <li>
                    <strong>Confirmer / Payer un règlement :</strong> dans le tableau des
                    règlements, utilisez les boutons « Confirm Settlement » pour confirmer
                    ou « Mark Paid » pour marquer comme payé avec une référence de paiement.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts et cycle de vie</h3>

                <p className="font-semibold mb-1">Commerçant</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">En attente</span></td><td className="p-2 border border-gray-700">Commerçant créé mais pas encore activé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Commerçant opérationnel, peut accepter des transactions.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Suspendu</span></td><td className="p-2 border border-gray-700">Commerçant temporairement désactivé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Résilié</span></td><td className="p-2 border border-gray-700">Commerçant définitivement désactivé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">En révision</span></td><td className="p-2 border border-gray-700">Commerçant en cours de vérification.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Terminal</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">Terminal opérationnel, prêt à traiter des transactions.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Inactif</span></td><td className="p-2 border border-gray-700">Terminal désactivé, ne traite plus de transactions.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Suspendu</span></td><td className="p-2 border border-gray-700">Terminal temporairement désactivé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Retiré</span></td><td className="p-2 border border-gray-700">Terminal retiré définitivement.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Défaillant</span></td><td className="p-2 border border-gray-700">Terminal signalé en panne.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Règlement</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">En attente</span></td><td className="p-2 border border-gray-700">Règlement créé, en attente de confirmation.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Confirmé</span></td><td className="p-2 border border-gray-700">Règlement confirmé par l'acquéreur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Payé</span></td><td className="p-2 border border-gray-700">Règlement payé au commerçant.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Contesté</span></td><td className="p-2 border border-gray-700">Règlement contesté par le commerçant ou l'acquéreur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Annulé</span></td><td className="p-2 border border-gray-700">Règlement annulé.</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Rôle de chaque bouton et champ</h3>
                <ul className="space-y-2">
                  <li>
                    <strong>3 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Merchants</em> — liste et création des commerçants.</li>
                      <li><em>Terminals</em> — terminaux du commerçant sélectionné, avec actions (Inject Keys).</li>
                      <li><em>Settlement</em> — règlements et netting du commerçant.</li>
                    </ul>
                  </li>
                  <li>
                    <strong>« Create Merchant »</strong> : ouvre le formulaire d'ajout d'un
                    commerçant. Remplir le nom, le MCC, le pays et sélectionner la banque
                    acquéreuse.
                  </li>
                  <li>
                    <strong>« Register Terminal »</strong> : ouvre le formulaire
                    d'enregistrement d'un terminal POS pour le commerçant sélectionné.
                  </li>
                  <li>
                    <strong>« Inject Keys »</strong> : injecte les clés de sécurité
                    (MKey, PIK, MAK) dans un terminal. Ces clés sont utilisées pour
                    l'authentification et le chiffrement des transactions.
                  </li>
                  <li>
                    <strong>« Create Settlement »</strong> : crée un règlement pour un
                    commerçant sur une date donnée.
                  </li>
                  <li>
                    <strong>« Load »</strong> : charge les règlements et le netting pour
                    un commerçant et une période donnés.
                  </li>
                  <li>
                    <strong>« Confirm Settlement »</strong> : confirme un règlement en
                    attente. Le statut passe de « En attente » à « Confirmé ».
                  </li>
                  <li>
                    <strong>« Mark Paid »</strong> : marque un règlement comme payé avec
                    une référence de paiement. Le statut passe de « Confirmé » à « Payé ».
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Qu'est-ce qu'un MCC ?</strong></p>
                  <p>
                    Le MCC (Merchant Category Code) est un code à 4 chiffres qui
                    catégorise le type d'activité du commerçant (ex. 5999 = services
                    divers, 5812 = restauration). Il détermine les taux d'interchange
                    et les règles applicables aux transactions.
                  </p>

                  <p><strong>Un terminal peut-il être réaffecté à un autre commerçant ?</strong></p>
                  <p>
                    Oui. Lors de l'enregistrement du terminal, sélectionnez le nouveau
                    commerçant. Le terminal sera lié à celui-ci. L'ancien commerçant
                    n'aura plus accès aux transactions de ce terminal.
                  </p>

                  <p><strong>À quoi servent les clés de sécurité (MKey, PIK, MAK) ?</strong></p>
                  <p>
                    MKey (Master Key) est la clé racine qui protège les autres clés.
                    PIK (PIN Encryption Key) chiffre le code PIN lors de la saisie sur
                    le terminal. MAK (MAC Authentication Key) calcule le code
                    d'authentification des messages (MAC) pour garantir l'intégrité
                    des transactions. Ces clés sont injectées lors de la mise en service
                    du terminal.
                  </p>

                  <p><strong>Quelle est la différence entre un règlement et un netting ?</strong></p>
                  <p>
                    Le <strong>règlement</strong> est le transfert de fonds individuel
                    pour un commerçant sur une période. Le <strong>netting</strong> est
                    le calcul de la position nette agrégée (débits bruts − crédits bruts)
                    qui détermine le montant final à payer ou à recevoir.
                  </p>

                  <p><strong>Peut-on annuler un règlement confirmé ?</strong></p>
                  <p>
                    Une fois confirmé ou payé, un règlement ne peut pas être annulé
                    depuis cette page. Il faut passer par les opérations de contrepassation
                    (disputes) ou contacter la banque acquéreuse.
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
