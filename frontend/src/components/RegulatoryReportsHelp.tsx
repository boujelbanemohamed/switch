import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const PERIODICITY_LABELS: Record<string, string> = {
  DAILY: 'Quotidien',
  MONTHLY: 'Mensuel',
};

export const FORMAT_LABELS: Record<string, string> = {
  CSV: 'CSV',
};

export function RegulatoryReportsHelp() {
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
              <h2 className="text-lg font-bold">Aide — Rapports réglementaires</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  La page <strong>Rapports réglementaires</strong> permet de consulter
                  les modèles de rapports destinés aux autorités de régulation (Banque Centrale
                  de Tunisie, SIBTEL) et de les générer au format CSV.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter les modèles de rapports disponibles</li>
                  <li>Générer un rapport pour une période donnée</li>
                  <li>Télécharger le rapport au format CSV</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Rapport réglementaire</strong> — un état périodique envoyé aux
                    autorités de tutelle (BCT, SIBTEL) contenant des données d'activité,
                    de volumes et de montants.</li>
                  <li><strong>Modèle (template)</strong> — un type de rapport prédéfini avec
                    son périmètre et sa périodicité. Chaque modèle correspond à un besoin
                    réglementaire spécifique.</li>
                  <li><strong>Période</strong> — l'intervalle de dates couvert par le rapport.
                    La date de début et de fin définissent les transactions incluses.</li>
                  <li><strong>Format CSV</strong> — le format de fichier produit (Comma-Separated
                    Values), lisible dans Excel et les outils d'analyse.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Générer un rapport</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Sélectionner un <strong>modèle</strong> dans la colonne de gauche
                        en cliquant sur la carte correspondante</li>
                      <li>Choisir la <strong>période</strong> (dates de début et fin) dans
                        la colonne de droite</li>
                      <li>Sélectionner le <strong>format</strong> de sortie (CSV uniquement)</li>
                      <li>Cliquer sur <strong>Générer et télécharger</strong></li>
                      <li>Le fichier est téléchargé automatiquement</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>

                <h4 className="font-medium mt-1 mb-1">Modèles de rapports</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Modèle</th>
                      <th className="p-1 text-left">Périodicité</th>
                      <th className="p-1 text-left">Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700">
                      <td className="p-1">BCT Daily Transaction Report</td>
                      <td className="p-1">Quotidien</td>
                      <td className="p-1">Récapitulatif quotidien pour la Banque Centrale</td>
                    </tr>
                    <tr className="border-t border-gray-700">
                      <td className="p-1">BCT Monthly Statistics</td>
                      <td className="p-1">Mensuel</td>
                      <td className="p-1">Statistiques mensuelles (volumes, montants)</td>
                    </tr>
                    <tr className="border-t border-gray-700">
                      <td className="p-1">SIBTEL Daily Switch Report</td>
                      <td className="p-1">Quotidien</td>
                      <td className="p-1">Activité quotidienne du switch pour SIBTEL</td>
                    </tr>
                    <tr className="border-t border-gray-700">
                      <td className="p-1">SIBTEL Monthly Settlement</td>
                      <td className="p-1">Mensuel</td>
                      <td className="p-1">Règlement mensuel pour les participants SIBTEL</td>
                    </tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Périodicités</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Code</th>
                      <th className="p-1 text-left">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">DAILY</td><td className="p-1">Quotidien — généré chaque jour ouvré</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MONTHLY</td><td className="p-1">Mensuel — généré en fin de mois</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Les rapports réglementaires permettent de produire les états exigés par
                  les autorités de régulation du système monétique tunisien.
                </p>

                <h4 className="font-medium mt-3 mb-1">État actuel</h4>
                <p className="text-gray-300">
                  Les 4 modèles de rapports sont définis et accessibles, mais la génération
                  des données réelles n'est pas encore implémentée. Actuellement, tous les
                  modèles produisent un fichier CSV qui contient uniquement les métadonnées
                  de la demande (nom du modèle, période, date de génération), sans aucune
                  donnée de transaction.
                </p>

                <h4 className="font-medium mt-3 mb-1">Fichier de règlement BCT</h4>
                <p className="text-gray-300">
                  Le fichier de règlement net destiné à la BCT (positions nettes par
                  institution) est disponible dans la page <strong>Compensation</strong>,
                  section « BCT Settlement ». Ce fichier CSV est généré à partir des données
                  réelles de compensation et exclut les participants étrangers.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quels rapports sont disponibles ?</dt>
                    <dd className="text-gray-300 mt-1">Quatre modèles sont prédéfinis : deux rapports BCT (quotidien et mensuel) et deux rapports SIBTEL (quotidien et mensuel). D'autres modèles pourront être ajoutés selon les besoins réglementaires.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quel est le format des fichiers générés ?</dt>
                    <dd className="text-gray-300 mt-1">Actuellement, seul le format CSV est supporté. Chaque rapport est téléchargé sous forme d'un fichier .csv directement depuis le navigateur.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les rapports contiennent-ils de vraies données ?</dt>
                    <dd className="text-gray-300 mt-1">Pas encore. La génération de rapports est en cours de développement. Les modèles sont prêts, mais ils ne sont pas encore branchés aux données de transaction. Les fichiers téléchargés contiennent pour l'instant les métadonnées de la demande (modèle, période, date de génération).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Où se trouve le fichier de règlement BCT ?</dt>
                    <dd className="text-gray-300 mt-1">Le fichier de règlement net BCT (positions nettes par banque) est disponible dans la page Compensation, section « BCT Settlement ». Il est généré à partir des données réelles de compensation et n'est pas lié aux modèles de cette page.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je programmer la génération automatique des rapports ?</dt>
                    <dd className="text-gray-300 mt-1">La génération programmée (quotidienne/mensuelle automatique) est prévue dans une version future. Actuellement, les rapports sont générés manuellement à la demande.</dd>
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
