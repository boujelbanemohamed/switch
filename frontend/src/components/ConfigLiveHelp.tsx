import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const CATEGORY_LABELS: Record<string, string> = {
  TRANSACTION: 'Transactions',
  AUTH: 'Authentification',
  FRAUD: 'Fraude',
  FEE: 'Frais',
  CLEARING: 'Compensation',
  MONITORING: 'Supervision',
  BATCH: 'Batch',
  KYC: 'KYC',
  SWITCH: 'Switch',
};

export const DATA_TYPE_LABELS: Record<string, string> = {
  INTEGER: 'Entier',
  LONG: 'Entier long',
  DECIMAL: 'Décimal',
  BOOLEAN: 'Booléen',
  STRING: 'Texte',
};

export function ConfigLiveHelp() {
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
              <h2 className="text-lg font-bold">Aide — Configuration live</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  La page <strong>Configuration live</strong> permet de consulter et modifier
                  les paramètres opérationnels du switch en temps réel, sans nécessiter
                  de redéploiement ni de redémarrage.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter tous les paramètres actifs, groupés par catégorie</li>
                  <li>Modifier la valeur d'un paramètre modifiable</li>
                  <li>Visualiser le type de donnée et la description de chaque paramètre</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Paramètre live</strong> — une configuration qui peut être lue
                    et modifiée pendant l'exécution du switch. Chaque paramètre a une clé
                    unique, une valeur, un type, une catégorie et un indicateur de
                    modification.</li>
                  <li><strong>Catégorie</strong> — un regroupement thématique des paramètres
                    (Transactions, Authentification, Fraude, etc.). Aide à naviguer
                    rapidement dans la liste.</li>
                   <li><strong>Type de donnée</strong> — le format attendu pour la valeur
                     (entier, décimal, booléen, texte). Le type est indicatif ; le backend
                     stocke la valeur comme texte sans la valider.</li>
                  <li><strong>Modifiable (mutable)</strong> — un paramètre peut être verrouillé
                    (non modifiable) ou ouvert à la modification. Les paramètres critiques
                    pour la sécurité sont généralement immutables.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier un paramètre</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Localiser le paramètre dans son groupe de catégorie</li>
                      <li>Cliquer sur le bouton <strong>Modifier</strong> dans la colonne Actions</li>
                      <li>Saisir la nouvelle valeur dans le champ d'édition</li>
                      <li>Cliquer sur le bouton <strong>OK</strong> (vert) pour enregistrer, ou <strong>X</strong> pour annuler</li>
                      <li>Le paramètre est mis à jour immédiatement et reflété dans le tableau</li>
                    </ol>
                  </div>
                  <div className="space-y-2">
                    <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300">
                      <p className="text-xs font-medium">Les modifications sont appliquées
                        en temps réel. Consultez le tableau ci-dessous pour comprendre
                        l'impact de chaque catégorie avant de modifier un paramètre.</p>
                    </div>
                    <table className="w-full text-xs text-gray-300 border border-gray-700">
                      <thead>
                        <tr className="bg-gray-800">
                          <th className="p-1 text-left">Catégorie</th>
                          <th className="p-1 text-left">Impact d'une modification</th>
                          <th className="p-1 text-left w-16">Risque</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Transactions</td><td className="p-1">Modifie les plafonds et délais d'attente pour les nouvelles transactions</td><td className="p-1 text-amber-400">Modéré</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Authentification</td><td className="p-1">Modifie les tentatives PIN, durée de session et limites d'auth.</td><td className="p-1 text-orange-400">Élevé</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Fraude</td><td className="p-1">Modifie les fenêtres de vélocité et seuils de détection</td><td className="p-1 text-amber-400">Modéré</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Frais</td><td className="p-1">Modifie les taux de commission et frais par défaut</td><td className="p-1 text-amber-400">Modéré</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Compensation</td><td className="p-1">Modifie les délais de règlement et seuils de netting</td><td className="p-1 text-amber-400">Modéré</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Supervision</td><td className="p-1">Modifie la fréquence d'actualisation et les seuils d'alerte</td><td className="p-1 text-gray-400">Faible</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">Batch</td><td className="p-1">Modifie les horaires d'exécution EOD/BOD</td><td className="p-1 text-amber-400">Modéré</td></tr>
                        <tr className="border-t border-gray-700"><td className="p-1 font-medium">KYC</td><td className="p-1">Modifie la taille max des documents et le niveau de vérification auto.</td><td className="p-1 text-gray-400">Faible</td></tr>
                        <tr className="border-t border-gray-700 bg-red-900/20"><td className="p-1 font-medium text-red-300">Switch</td><td className="p-1"><span className="text-red-400">Critique :</span> <code>maintenance_mode=true</code> rejette immédiatement toutes les transactions. <code>logging_level</code> modifie la verbosité des logs.</td><td className="p-1 text-red-400 font-bold">Critique</td></tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>

                <h4 className="font-medium mt-1 mb-1">Catégories</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Catégorie</th>
                      <th className="p-1 text-left">Description</th>
                      <th className="p-1 text-left">Nb paramètres</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">TRANSACTION</td><td className="p-1">Plafonds et timeouts transactionnels</td><td className="p-1">3</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">AUTH</td><td className="p-1">Configuration de l'authentification</td><td className="p-1">3</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">FRAUD</td><td className="p-1">Seuils et fenêtres de détection fraude</td><td className="p-1">3</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">FEE</td><td className="p-1">Taux de frais par défaut</td><td className="p-1">3</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">CLEARING</td><td className="p-1">Paramètres de compensation</td><td className="p-1">2</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">MONITORING</td><td className="p-1">Seuils de supervision et alertes</td><td className="p-1">3</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BATCH</td><td className="p-1">Horaires d'exécution des batchs</td><td className="p-1">2</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">KYC</td><td className="p-1">Paramètres KYC</td><td className="p-1">2</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">SWITCH</td><td className="p-1">Configuration générale du switch</td><td className="p-1">2</td></tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Types de données</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Type</th>
                      <th className="p-1 text-left">Exemple</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">INTEGER</td><td className="p-1">30000 (timeout en ms)</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">LONG</td><td className="p-1">1000000 (montant max)</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">DECIMAL</td><td className="p-1">0.012 (taux 1,2%)</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BOOLEAN</td><td className="p-1">true / false</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">STRING</td><td className="p-1">INFO (niveau de log)</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Les paramètres sont stockés dans la table <code>live_config</code> de la
                  base de données. Lors du démarrage du switch, les paramètres sont chargés
                  en mémoire cache pour un accès rapide.
                </p>

                <h4 className="font-medium mt-3 mb-1">Mise à jour</h4>
                <p className="text-gray-300">
                  Lorsqu'un paramètre est modifié via cette interface, le backend :
                </p>
                <ol className="list-decimal list-inside text-gray-300 space-y-1 mt-1">
                  <li>Persiste la nouvelle valeur en base de données</li>
                  <li>Journalise la modification dans les logs applicatifs (utilisateur, date, nouvelle valeur)</li>
                </ol>

                <h4 className="font-medium mt-3 mb-1">Paramètres non modifiables</h4>
                <p className="text-gray-300">
                  Certains paramètres critiques sont marqués comme non modifiables
                  (<code>mutable = false</code>). Ces paramètres ne peuvent être changés
                  que par déploiement. Le bouton Modifier n'apparaît pas pour ces
                  paramètres.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Les modifications sont-elles immédiates ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, les changements sont appliqués en temps réel. La transaction suivante utilisera la nouvelle valeur. Aucun redémarrage du switch n'est nécessaire.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je revenir à la valeur précédente ?</dt>
                    <dd className="text-gray-300 mt-1">Cette interface ne gère pas l'historique. Notez la valeur initiale avant modification si vous souhaitez pouvoir la restaurer. Seule la nouvelle valeur est conservée dans les logs applicatifs, sans table d'audit dédiée.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que faire si je saisis une valeur invalide ?</dt>
                    <dd className="text-gray-300 mt-1">Le backend stocke toute valeur comme texte sans la valider. Si vous saisissez <code>abc</code> dans un paramètre attendu comme nombre, la modification est acceptée silencieusement — le composant qui lit ce paramètre obtiendra la valeur par défaut (souvent 0). Aucune erreur ne vous prévient. Vérifiez toujours le format de la valeur avant de valider.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Qui peut modifier les paramètres ?</dt>
                    <dd className="text-gray-300 mt-1">Seuls les utilisateurs avec le rôle ADMIN ou OPERATOR peuvent modifier les paramètres. Les utilisateurs ANALYST peuvent les consulter mais pas les éditer.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les paramètres survivent-ils à un redémarrage ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, les modifications sont persistées en base de données. Après un redémarrage du switch, les dernières valeurs enregistrées sont chargées et appliquées.</dd>
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
