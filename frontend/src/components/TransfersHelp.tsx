import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export const TRANSFER_LABELS: Record<string, string> = {
  COMPLETED: 'Effectué',
  REVERSED: 'Annulé',
  FAILED: 'Échoué',
  PENDING: 'En attente',
};

export function TransfersHelp() {
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
              <h2 className="text-lg font-bold">Aide — Virements</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page permet d'effectuer des virements entre comptes internes (A2A, Account-to-Account)
                  et entre porteurs de cartes (P2P, Peer-to-Peer). Elle donne aussi accès à l'historique des
                  transferts, aux plafonds applicables et à la gestion des bénéficiaires.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Virement A2A (Account-to-Account) :</strong> transfert entre deux comptes internes
                    de la plateforme, identifiés par leur UUID. Utilisé pour des mouvements entre vos propres
                    comptes ou entre participants.
                  </li>
                  <li>
                    <strong>Virement P2P (Peer-to-Peer) :</strong> transfert entre deux porteurs de cartes.
                    Le donneur d'ordre est identifié par les derniers chiffres de sa carte (suffixe PAN),
                    le destinataire par un UUID de compte.
                  </li>
                  <li>
                    <strong>Frais de transfert :</strong> des frais peuvent être appliqués selon le type de
                    virement (configurable dans les paramètres système). Les virements A2A coûtent 5 TND,
                    les virements P2P coûtent 2 TND.
                  </li>
                  <li>
                    <strong>Plafonds :</strong> chaque type de virement a des limites : montant maximum par
                    virement, montant cumulé maximum par jour et nombre maximum d'opérations par jour.
                  </li>
                  <li>
                    <strong>Annulation (reverse) :</strong> un virement effectué (COMPLETED) peut être annulé
                    tant qu'il n'est pas lui-même le résultat d'une annulation. L'annulation rembourse le
                    montant et les frais.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Consultez l'historique</strong> (onglet « Historique ») pour voir tous les
                    transferts effectués, leur montant, leurs frais, leur statut et la date.
                  </li>
                  <li>
                    <strong>Effectuez un virement A2A</strong> (onglet « Virement A2A ») : saisissez l'UUID
                    du compte source, l'UUID du compte destinataire, le montant et la devise. Cliquez sur
                    « Exécuter ».
                  </li>
                  <li>
                    <strong>Effectuez un virement P2P</strong> (onglet « Virement P2P ») : saisissez le
                    suffixe (8 derniers chiffres) de la carte source, l'UUID ou le RIB du destinataire,
                    le montant et la devise. Cliquez sur « Exécuter ».
                  </li>
                  <li>
                    <strong>Consultez les plafonds</strong> (onglet « Plafonds ») pour voir les limites
                    applicables à chaque type de transfert.
                  </li>
                  <li>
                    <strong>Annulez un virement</strong> : dans l'historique, cliquez sur « Annuler » à
                    côté d'un virement au statut « Effectué ». Un motif vous est demandé.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts des virements</h3>
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
                      <td className="p-2 border border-gray-700">En cours de traitement. Le transfert n'est pas encore finalisé.</td>
                    </tr>
                    <tr>
                      <td className="p-2 border border-gray-700">
                        <span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Effectué (COMPLETED)</span>
                      </td>
                      <td className="p-2 border border-gray-700">Le transfert a réussi. Les fonds ont été déplacés.</td>
                    </tr>
                    <tr>
                      <td className="p-2 border border-gray-700">
                        <span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Annulé (REVERSED)</span>
                      </td>
                      <td className="p-2 border border-gray-700">Le transfert a été annulé. Les fonds ont été remboursés.</td>
                    </tr>
                    <tr>
                      <td className="p-2 border border-gray-700">
                        <span className="px-2 py-0.5 rounded bg-amber-900/50 text-amber-400">Échoué (FAILED)</span>
                      </td>
                      <td className="p-2 border border-gray-700">Le transfert a échoué (solde insuffisant, compte invalide, etc.).</td>
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
                    <strong>Onglets de navigation</strong> : Historique (liste des transferts), Virement A2A
                    (formulaire), Virement P2P (formulaire), Plafonds (limites par type).
                  </li>
                  <li>
                    <strong>Bouton « Exécuter »</strong> (formulaires A2A/P2P) : déclenche le virement avec les
                    paramètres saisis. Désactivé pendant le traitement.
                  </li>
                  <li>
                    <strong>Bouton « Annuler »</strong> (dans l'historique) : apparaît uniquement pour les
                    virements au statut « Effectué » qui ne sont pas déjà des annulations. Ouvre une boîte de
                    dialogue pour saisir le motif.
                  </li>
                  <li>
                    <strong>Champs du formulaire A2A :</strong> Compte source (UUID), Compte destinataire (UUID),
                    Montant, Devise.
                  </li>
                  <li>
                    <strong>Champs du formulaire P2P :</strong> N° de carte source (8 derniers chiffres),
                    Destinataire (UUID ou RIB), Montant, Devise.
                  </li>
                  <li>
                    <strong>Tableau Historique :</strong> colonnes Date, Type (A2A/P2P), Montant, Frais,
                    Statut (badge coloré), action Annuler.
                  </li>
                  <li>
                    <strong>Tableau Plafonds :</strong> colonnes Type, Maximum par virement, Plafond journalier,
                    Nombre max par jour.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Quelle est la différence entre A2A et P2P ?</strong></p>
                  <p>
                    L'A2A (Account-to-Account) transfère des fonds entre deux comptes internes identifiés par
                    leur UUID. Le P2P (Peer-to-Peer) transfère entre deux porteurs de cartes : le donneur
                    d'ordre est identifié par les derniers chiffres de sa carte.
                  </p>

                  <p><strong>Pourquoi mon virement affiche-t-il « Échoué » ?</strong></p>
                  <p>
                    Les causes possibles : solde insuffisant sur le compte source, compte destinataire
                    inexistant ou inactif, devise incompatible, ou dépassement des plafonds.
                  </p>

                  <p><strong>Puis-je annuler un virement ?</strong></p>
                  <p>
                    Oui, si son statut est « Effectué » (COMPLETED) et qu'il n'est pas déjà une annulation.
                    Cliquez sur « Annuler » dans l'historique et saisissez un motif. L'annulation rembourse
                    le montant et les frais.
                  </p>

                  <p><strong>Où voir les plafonds applicables ?</strong></p>
                  <p>
                    Dans l'onglet « Plafonds ». Les limites sont configurables dans les paramètres système
                    et s'appliquent par type de virement (A2A/P2P).
                  </p>

                  <p><strong>Des frais sont-ils appliqués ?</strong></p>
                  <p>
                    Oui. Les virements A2A sont facturés 5 TND, les virements P2P 2 TND. Ces montants sont
                    configurables dans les paramètres du système.
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
