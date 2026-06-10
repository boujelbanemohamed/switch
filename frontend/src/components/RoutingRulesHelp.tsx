import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const PROTOCOL_LABELS: Record<string, string> = {
  ISO8583: 'ISO 8583',
  ISO20022: 'ISO 20022',
  BOTH: 'Les deux',
};

export const RULE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
};

export function RoutingRulesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Regles de Routage</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. A quoi sert cette page</h3>
                <p>
                  Les <strong>regles de routage</strong> determinent vers quel participant (banque,
                  processeur) une transaction est dirigee en fonction de ses caracteristiques :
                  marque de carte, pays, devise, protocole, etc.
                </p>
                <p className="mt-2">
                  Cette page permet de gerer l'ensemble des regles qui constituent le moteur de
                  routage du switch. C'est ici que l'on definit quel chemin emprunte chaque
                  transaction.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter la liste des regles existantes (nom, priorite, source, destination, protocole)</li>
                  <li>Creer, modifier, activer/desactiver et supprimer des regles de routage</li>
                  <li>Visualiser les participants disponibles pour chaque regle</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Regle de routage</strong> — une instruction qui associe une transaction
                    entrante a un participant de destination. Chaque regle a une priorite, des
                    conditions, une source, une destination, un protocole et un type de message.</li>
                  <li><strong>Priorite</strong> — plus le chiffre est bas, plus la regle est evaluee
                    tot (1 avant 100). La premiere regle dont les conditions correspondent est appliquee
                    (les suivantes ignorees). Par defaut : 100.</li>
                  <li><strong>Participant source</strong> — l'emetteur de la transaction (optionnel).
                    Si vide, la regle s'applique a toutes les sources.</li>
                  <li><strong>Participant destination</strong> — le destinataire vers qui la transaction
                    est acheminee si la regle correspond. Champ obligatoire.</li>
                  <li><strong>Protocole</strong> — le format du message : ISO 8583 (messages financiers
                    historiques), ISO 20022 (format XML moderne), ou les deux.</li>
                  <li><strong>Type de message</strong> — filtre optionnel sur le MTI (Message Type
                    Identifier), ex: <code>0100</code> pour une autorisation, <code>0200</code>
                    pour une transaction financiere.</li>
                  <li><strong>Expression de condition</strong> — une structure JSON qui definit les
                    criteres de matching. Le format attendu est :
                    <br />
                    <code className="block mt-1 whitespace-pre-wrap">{'{'}&quot;operator&quot;: &quot;AND&quot;, &quot;rules&quot;: [
  {'{'}&quot;field&quot;: &quot;BIN&quot;, &quot;operator&quot;: &quot;STARTS_WITH&quot;, &quot;value&quot;: &quot;550000&quot;{'}'}
{']'}{'}'}</code>
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Creer une regle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Regles de Routage</strong> (bouton bleu en haut a droite)</li>
                      <li>Saisir le <strong>Nom</strong> de la regle (ex: <code>Visa Tunisie</code>)</li>
                      <li>Saisir la <strong>Description</strong> (optionnelle)</li>
                      <li>Definir la <strong>Priorite</strong> (plus petit = plus prioritaire)</li>
                      <li>Selectionner le <strong>Protocole</strong> (ISO 8583, ISO 20022, ou les deux)</li>
                      <li>Saisir l'<strong>Expression de condition</strong> en JSON.
                        Exemple pour un montant maximum de 10 000 TND :
                        <br />
                        <code className="block mt-1">{'{'}&quot;operator&quot;: &quot;AND&quot;, &quot;rules&quot;: [
  {'{'}&quot;field&quot;: &quot;AMOUNT&quot;, &quot;operator&quot;: &quot;AMOUNT_RANGE&quot;, &quot;min&quot;: &quot;0&quot;, &quot;max&quot;: &quot;10000&quot;{'}'}
{']'}{'}'}</code>
                      </li>
                      <li>Choisir le <strong>Participant source</strong> (optionnel — laisser vide pour toutes les sources)</li>
                      <li>Choisir le <strong>Participant destination</strong> (obligatoire)</li>
                      <li>Saisir le <strong>Type de message</strong> (optionnel)</li>
                      <li>Choisir le <strong>Statut</strong> (Actif ou Inactif)</li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier une regle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone crayon de la regle ciblee</li>
                      <li>Modifier les champs souhaites</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong> pour valider</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Activer / Desactiver</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone toggle de la regle ciblee</li>
                      <li>Une regle desactivee n'est pas evaluee par le moteur de routage</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer une regle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone poubelle de la regle ciblee</li>
                      <li>Confirmer la suppression dans la boite de dialogue</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>
                <h4 className="font-medium mt-1 mb-1">Protocoles</h4>
                <ul className="space-y-1">
                  <li><strong>ISO 8583</strong> — format historique des messages financiers (messages binaires). Utilise pour les transactions en temps reel (autorisation, compensation).</li>
                  <li><strong>ISO 20022</strong> — format XML moderne pour les virements, le clearing et les rapports. Plus riche mais plus verbeux.</li>
                  <li><strong>Les deux</strong> — la regle s'applique quel que soit le protocole du message entrant.</li>
                </ul>
                <h4 className="font-medium mt-3 mb-1">Statuts</h4>
                <ul className="space-y-1">
                  <li><span className="text-green-500 font-medium">Actif</span> — la regle est prise en compte par le moteur de routage</li>
                  <li><span className="text-gray-400 font-medium">Inactif</span> — la regle existe mais n'est pas evaluee</li>
                </ul>
                <h4 className="font-medium mt-3 mb-1">Operateurs de condition</h4>
                <p className="text-gray-300">
                  L'expression de condition utilise le format JSON. Le moteur supporte les operateurs suivants :
                </p>
                <ul className="space-y-2 text-gray-300 mt-1">
                  <li>
                    <code>EQUALS</code> — egalite exacte.<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;CURRENCY&quot;, &quot;operator&quot;: &quot;EQUALS&quot;, &quot;value&quot;: &quot;TND&quot;{'}'}</code>
                  </li>
                  <li>
                    <code>NOT_EQUALS</code> — different de.<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;CURRENCY&quot;, &quot;operator&quot;: &quot;NOT_EQUALS&quot;, &quot;value&quot;: &quot;EUR&quot;{'}'}</code>
                  </li>
                  <li>
                    <code>STARTS_WITH</code> — commence par (utilise pour les BIN).<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;BIN&quot;, &quot;operator&quot;: &quot;STARTS_WITH&quot;, &quot;value&quot;: &quot;550000&quot;{'}'}</code>
                  </li>
                  <li>
                    <code>CONTAINS</code> — contient la valeur.<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;PAN&quot;, &quot;operator&quot;: &quot;CONTAINS&quot;, &quot;value&quot;: &quot;1234&quot;{'}'}</code>
                  </li>
                  <li>
                    <code>IN</code> — appartient a une liste de valeurs.<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;CURRENCY&quot;, &quot;operator&quot;: &quot;IN&quot;, &quot;values&quot;: [&quot;TND&quot;, &quot;EUR&quot;]{'}'}</code>
                  </li>
                  <li>
                    <code>BIN_RANGE</code> — le BIN est dans un intervalle numerique.<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;BIN&quot;, &quot;operator&quot;: &quot;BIN_RANGE&quot;, &quot;min&quot;: &quot;400000&quot;, &quot;max&quot;: &quot;499999&quot;{'}'}</code>
                  </li>
                  <li>
                    <code>AMOUNT_RANGE</code> — le montant est dans un intervalle (min et/ou max).<br />
                    <code className="block mt-1">{'{'}&quot;field&quot;: &quot;AMOUNT&quot;, &quot;operator&quot;: &quot;AMOUNT_RANGE&quot;, &quot;min&quot;: &quot;1000&quot;, &quot;max&quot;: &quot;50000&quot;{'}'}</code>
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Lorsqu'une transaction arrive dans le switch, le moteur de routage suit ces etapes :
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Il charge toutes les regles actives, triees par priorite croissante (1 avant 100), en filtrant par protocole (seules les regles du protocole de la transaction et celles en mode « Les deux » sont chargees)</li>
                  <li>Pour chaque regle <strong>dans l'ordre de priorite</strong>, il evalue l'expression de condition</li>
                  <li>Si l'expression est vraie, la regle est appliquee et le routage s'arrete — les regles de priorite plus faible ne sont pas examinees</li>
                  <li>Si aucune regle ne correspond, le resultat est <code>non route</code> (la transaction n'est pas acheminee automatiquement)</li>
                </ol>
                <p className="mt-2">
                  <strong>Format de l'expression de condition</strong> — le JSON attendu a toujours cette structure :
                </p>
                <code className="block mt-1 whitespace-pre-wrap">{'{'}
  &quot;operator&quot;: &quot;AND&quot;,
  &quot;rules&quot;: [
    {'{'} &quot;field&quot;: &quot;...&quot;, &quot;operator&quot;: &quot;...&quot;, &quot;value&quot;: &quot;...&quot; {'}'}
  ]
{'}'}</code>
                <p className="mt-2">
                  <code>operator</code> peut etre <code>AND</code> (toutes les conditions doivent etre vraies)
                  ou <code>OR</code> (au moins une condition vraie). Chaque condition dans <code>rules</code>
                  est un objet avec <code>field</code>, <code>operator</code>, et selon l'operateur :
                  <code>value</code>, <code>min</code>/<code>max</code>, ou <code>values</code> (liste).
                </p>
                <p className="mt-2">
                  Exemple avec deux conditions en ET :
                </p>
                <code className="block mt-1 whitespace-pre-wrap">{'{'}
  &quot;operator&quot;: &quot;AND&quot;,
  &quot;rules&quot;: [
    {'{'} &quot;field&quot;: &quot;BIN&quot;, &quot;operator&quot;: &quot;STARTS_WITH&quot;, &quot;value&quot;: &quot;550000&quot; {'}'},
    {'{'} &quot;field&quot;: &quot;CURRENCY&quot;, &quot;operator&quot;: &quot;EQUALS&quot;, &quot;value&quot;: &quot;TND&quot; {'}'}
  ]
{'}'}</code>
                <p className="mt-2">
                  Les champs disponibles pour les conditions sont :
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li><code>BIN</code> — les 6 a 8 premiers chiffres du PAN</li>
                  <li><code>PAN</code> — le numero de carte complet</li>
                  <li><code>AMOUNT</code> — le montant de la transaction</li>
                  <li><code>CURRENCY</code> — le code devise (ex: TND, EUR)</li>
                  <li><code>MTI</code> ou <code>MESSAGE_TYPE</code> — le type de message (ex: 0100, 0200)</li>
                  <li><code>MERCHANT_ID</code> — l'identifiant du marchand</li>
                  <li><code>TERMINAL_ID</code> — l'identifiant du terminal</li>
                  <li><code>SOURCE</code> — le participant source</li>
                  <li><code>PROTOCOL</code> — le protocole (ISO8583, ISO20022)</li>
                  <li><code>COUNTRY</code> — le code pays</li>
                </ul>
                <p className="mt-2">
                  <strong>Regle attrape-tout (fallback)</strong> — pour eviter qu'une transaction ne soit pas
                  routee, creer une regle avec une priorite elevee (ex: 9999), une condition vide
                  (laisser le champ conditionExpression vide), et le participant de destination par defaut.
                  Cette regle sera appliquee si aucune regle plus specifique ne correspond.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre une regle de routage et une regle stand-in ?</dt>
                    <dd className="text-gray-300 mt-1">La regle de routage determine vers quel participant envoyer la transaction. Elle est toujours utilisee. Le stand-in est un filet de secours declenche uniquement si le participant de destination est injoignable — il ne remplace pas le routage.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si deux regles ont la meme priorite ?</dt>
                    <dd className="text-gray-300 mt-1">La premieres trouvee dans la base de donnees est utilisee, mais l'ordre n'est pas garanti. Il est recommande d'utiliser des priorites distinctes pour eviter toute ambiguite.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je router vers un participant exterieur (hors plateforme) ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, si le participant de destination est configure comme passerelle externe (ex: processeur europeen en ISO 20022). Le protocole de la regle doit correspondre au format attendu par ce participant.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Comment creer une regle « attrape-tout » ?</dt>
                    <dd className="text-gray-300 mt-1">Créez une regle avec une priorite basse (ex: 9999 pour qu'elle soit evaluee en dernier), une condition vide (laissez le champ expression vide), et le participant de destination par defaut. Cette regle sera appliquee si aucune regle plus specifique ne correspond.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Une regle desactivee est-elle conservee ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Passer une regle en inactif la desactive sans la supprimer. Elle reapparaitra dans le routage si vous la reactivez. Cela permet de mettre des regles de cote temporairement sans perdre leur configuration.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quel format pour l'expression de condition ?</dt>
                    <dd className="text-gray-300 mt-1">
                      <p>Le format JSON attendu est toujours :</p>
                      <code className="block mt-1 whitespace-pre-wrap">{'{'}
  &quot;operator&quot;: &quot;AND&quot;,
  &quot;rules&quot;: [
    {'{'} &quot;field&quot;: &quot;BIN&quot;, &quot;operator&quot;: &quot;STARTS_WITH&quot;, &quot;value&quot;: &quot;4765&quot; {'}'}
  ]
{'}'}</code>
                      <p className="mt-2">Exemple avec intervalle de montant :</p>
                      <code className="block mt-1 whitespace-pre-wrap">{'{'}
  &quot;operator&quot;: &quot;AND&quot;,
  &quot;rules&quot;: [
    {'{'} &quot;field&quot;: &quot;AMOUNT&quot;, &quot;operator&quot;: &quot;AMOUNT_RANGE&quot;, &quot;min&quot;: &quot;1000&quot;, &quot;max&quot;: &quot;50000&quot; {'}'}
  ]
{'}'}</code>
                      <p className="mt-2">
                        Les formats simplifies comme <code>{'{'}&quot;BIN&quot;: &quot;4765&quot;{'}'}</code> ne sont PAS acceptes.
                        Utilisez toujours la structure <code>operator</code> + <code>rules</code> avec
                        <code>field</code>, <code>operator</code> et <code>value</code>.
                      </p>
                    </dd>
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
