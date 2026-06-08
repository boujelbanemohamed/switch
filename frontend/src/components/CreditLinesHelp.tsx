import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export const CREDIT_LINE_LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  BLOCKED: 'Bloquée',
  CLOSED: 'Fermée',
  OPEN: 'Ouverte',
};

export const STATEMENT_LABELS: Record<string, string> = {
  OPEN: 'Ouvert',
  PAID: 'Payé',
  OVERDUE: 'En retard',
};

export const INSTALLMENT_LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  COMPLETED: 'Terminée',
  DEFAULTED: 'En défaut',
  CANCELLED: 'Annulée',
};

export function CreditLinesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Crédit Revolving</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère les <strong>lignes de crédit revolving</strong> : ouverture, autorisation,
                  achat, paiement, relevés de facturation, échéanciers et simulation d'intérêts. Chaque ligne
                  de crédit est liée à un compte carte et fonctionne comme une réserve d'argent renouvelable
                  — au fur et à mesure des remboursements, le crédit disponible se reconstitué.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Ligne de crédit :</strong> montant maximum mis à disposition d'un porteur
                    (<em>creditLimit</em>). Le solde utilisé (<em>currentBalance</em>) ne peut pas dépasser
                    cette limite.
                  </li>
                  <li>
                    <strong>Crédit disponible :</strong> différence entre le plafond et le solde utilisé,
                    moins les autorisations en attente (<em>holdAmount</em>).
                  </li>
                  <li>
                    <strong>Relevé de facturation :</strong> document périodique (généré mensuellement) qui
                    récapitule les achats, les intérêts, le solde de clôture, le paiement minimum et la
                    date d'échéance.
                  </li>
                  <li>
                    <strong>Période de grâce :</strong> si le relevé précédent a été payé intégralement,
                    aucun intérêt n'est facturé sur le nouveau relevé. Les intérêts ne s'appliquent que
                    lorsqu'un solde est reporté d'une période à l'autre.
                  </li>
                  <li>
                    <strong>Paiement minimum :</strong> montant plancher à rembourser chaque période,
                    calculé comme <em>max(solde de clôture × %, plancher)</em>. Si seul le minimum est
                    payé, le solde restant porte intérêt.
                  </li>
                  <li>
                    <strong>Échéancier (installments) :</strong> conversion d'un achat en N mensualités
                    fixes avec frais et taux d'intérêt dédié. Chaque échéance peut être réglée
                    individuellement.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Ouvrir une ligne :</strong> cliquez sur « Ouvrir une ligne » en haut à droite.
                    Sélectionnez un compte carte, définissez le plafond, le taux APR, le jour de relevé,
                    le délai de paiement et les paramètres de paiement minimum.
                  </li>
                  <li>
                    <strong>Sélectionnez une ligne :</strong> cliquez sur une carte pour afficher ses
                    détails. Le panneau du bas affiche 4 onglets : Détails, Relevés, Échéanciers,
                    Simulation.
                  </li>
                  <li>
                    <strong>Autoriser un montant :</strong> dans l'onglet Détails, saisissez un montant
                    et cliquez sur « Autoriser ». Cela pose un hold sans débiter le compte (utile pour
                    une réservation).
                  </li>
                  <li>
                    <strong>Effectuer un achat :</strong> saisissez le montant et une référence, puis
                    cliquez sur « Achat ». Le montant est débité de la ligne et une écriture ledger
                    est générée (CREDIT_RECEIVABLE / CREDIT_FUNDING).
                  </li>
                  <li>
                    <strong>Effectuer un paiement :</strong> saisissez un montant et cliquez sur
                    « Paiement ». Le solde de la ligne diminue et le crédit disponible augmente.
                  </li>
                  <li>
                    <strong>Générer un relevé :</strong> dans l'onglet Relevés, cliquez sur « Générer
                    un relevé ». Le système calcule le solde d'ouverture, les achats, les intérêts
                    (si grâce expirée) et le paiement minimum.
                  </li>
                  <li>
                    <strong>Convertir en échéancier :</strong> dans l'onglet Échéanciers, saisissez
                    le montant, le nombre d'échéances, les frais et l'APR (optionnel), puis cliquez
                    sur « Convertir ».
                  </li>
                  <li>
                    <strong>Simuler :</strong> dans l'onglet Simulation, ajustez le solde, l'APR,
                    le pourcentage minimum et le plancher pour visualiser les intérêts et le
                    paiement estimé.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  4. Statuts et cycle de vie
                </h3>

                <p className="font-semibold mb-1">Ligne de crédit</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Active</span></td><td className="p-2 border border-gray-700">La ligne est ouverte et utilisable. Les autorisations, achats et paiements sont possibles.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Inactive</span></td><td className="p-2 border border-gray-700">La ligne existe mais n'est pas activée. Aucune opération n'est possible.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Bloquée</span></td><td className="p-2 border border-gray-700">La ligne a été bloquée (impayé, anomalie). Les opérations sont suspendues.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-500">Fermée</span></td><td className="p-2 border border-gray-700">La ligne est définitivement fermée. Solde doit être nul.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Relevé de facturation</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Ouvert</span></td><td className="p-2 border border-gray-700">Relevé généré, en attente de paiement. Le porteur a jusqu'à la date d'échéance pour payer.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Payé</span></td><td className="p-2 border border-gray-700">Relevé réglé intégralement. Le solde de clôture a été payé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">En retard</span></td><td className="p-2 border border-gray-700">La date d'échéance est dépassée sans paiement intégral. Des pénalités peuvent s'appliquer.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Échéancier</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Active</span></td><td className="p-2 border border-gray-700">L'échéancier est en cours. Des échéances restent à payer.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-500">Terminée</span></td><td className="p-2 border border-gray-700">Toutes les échéances ont été réglées. L'échéancier est soldé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">En défaut</span></td><td className="p-2 border border-gray-700">Une ou plusieurs échéances n'ont pas été payées à temps.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Annulée</span></td><td className="p-2 border border-gray-700">L'échéancier a été annulé avant son terme.</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">
                  5. Rôle de chaque bouton et champ
                </h3>
                <ul className="space-y-2">
                  <li>
                    <strong>« Ouvrir une ligne »</strong> — bouton en haut à droite : ouvre une modale
                    pour créer une nouvelle ligne de crédit avec plafond, APR, jour de relevé, délai
                    de paiement et paramètres de paiement minimum.
                  </li>
                  <li>
                    <strong>« Autoriser »</strong> — réserve un montant sur la ligne (hold) sans le
                    débiter. Utile pour les pré-autorisations. Le montant est bloqué jusqu'à
                    libération ou achat.
                  </li>
                  <li>
                    <strong>« Achat »</strong> — débite le montant de la ligne et crée une écriture
                    comptable (CREDIT_RECEIVABLE / CREDIT_FUNDING). Le crédit disponible diminue.
                  </li>
                  <li>
                    <strong>« Paiement »</strong> — rembourse une partie ou la totalité du solde.
                    Le crédit disponible augmente. Si le relevé est payé intégralement, le statut
                    passe à « Payé ».
                  </li>
                  <li>
                    <strong>« Générer un relevé »</strong> — produit un relevé de facturation avec
                    le solde d'ouverture, les achats, les intérêts calculés et le paiement minimum.
                  </li>
                  <li>
                    <strong>« Simuler »</strong> — calcule les intérêts et le paiement minimum pour
                    un scénario donné (solde, APR, pourcentage min, plancher). Aide à la
                    planification.
                  </li>
                  <li>
                    <strong>« Convertir »</strong> — transforme un montant en échéancier avec N
                    mensualités, frais éventuels et APR dédié. Les échéances sont générées et
                    affichées dans la liste.
                  </li>
                  <li>
                    <strong>4 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Détails</em> — vue synthétique avec plafond, solde, disponible. Actions Autoriser/Achat/Paiement.</li>
                      <li><em>Relevés</em> — historique des relevés avec périodes, soldes et statuts.</li>
                      <li><em>Échéanciers</em> — plans d'amortissement avec échéances individuelles et statut par ligne.</li>
                      <li><em>Simulation</em> — calculateur d'intérêts et de paiement minimum.</li>
                    </ul>
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Comment sont calculés les intérêts ?</strong></p>
                  <p>
                    Les intérêts = <em>solde d'ouverture du relevé × (APR / 12 / 100)</em>.
                    Ils ne sont facturés que si le relevé précédent n'a pas été payé intégralement
                    (période de grâce perdue). Si le relevé précédent est payé en totalité avant
                    l'échéance, aucun intérêt n'est dû sur la période suivante.
                  </p>

                  <p><strong>Qu'est-ce que le paiement minimum ?</strong></p>
                  <p>
                    C'est le montant plancher à payer avant la date d'échéance pour éviter le statut
                    « En retard ». Il est calculé comme <em>max(solde de clôture × %, plancher)</em>.
                    Si seul le minimum est payé, le solde restant est reporté et porte intérêt.
                  </p>

                  <p><strong>Puis-je avoir plusieurs lignes de crédit ?</strong></p>
                  <p>
                    Oui, chaque ligne est liée à un compte carte différent. Vous pouvez en ouvrir
                    plusieurs et les gérer indépendamment.
                  </p>

                  <p><strong>Que signifie un échéancier « En défaut » ?</strong></p>
                  <p>
                    Cela signifie qu'au moins une échéance n'a pas été réglée à sa date d'échéance.
                    Le plan d'amortissement est considéré comme en défaut et des pénalités
                    contractuelles peuvent s'appliquer.
                  </p>

                  <p><strong>La simulation utilise-t-elle les mêmes règles que le réel ?</strong></p>
                  <p>
                    Oui, la simulation applique la même formule d'intérêts et de paiement minimum
                    que le moteur de calcul réel. Les résultats sont indicatifs et basés sur les
                    paramètres que vous saisissez.
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
