import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const PARTICIPANT_TYPE_LABELS: Record<string, string> = {
  ACQUIRER: 'Acquéreur',
  ISSUER: 'Émetteur',
  SWITCH: 'Switch',
  PROCESSOR: 'Processeur',
};

export const PARTICIPANT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  SUSPENDED: 'Suspendu',
};

export const ENDPOINT_TYPE_LABELS: Record<string, string> = {
  TCP: 'TCP/IP',
  HTTP: 'HTTP/REST',
  MQ: 'File de messages',
  FILE: 'Fichier',
};

export function ParticipantsHelp() {
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
              <h2 className="text-lg font-bold">Aide — Participants</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  La page <strong>Participants</strong> permet de gérer l'annuaire des institutions
                  qui participent au système monétique. Chaque participant représente une entité
                  avec laquelle le switch communique pour traiter les transactions.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter la liste de tous les participants</li>
                  <li>Ajouter et modifier des participants</li>
                  <li>Chaque participant est identifié par un code unique et appartient à un type</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Acquéreur</strong> — l'institution qui détient la relation commerciale
                    avec le marchand. Elle envoie les transactions au switch pour traitement.</li>
                  <li><strong>Émetteur</strong> — l'institution qui a émis la carte du porteur.
                    Elle reçoit les demandes d'autorisation et règle les transactions.</li>
                  <li><strong>Switch</strong> — le système central qui achemine les transactions
                    entre les participants. Il joue le rôle d'intermédiaire technique.</li>
                  <li><strong>Processeur</strong> — un prestataire technique externe qui traite
                    les transactions pour le compte d'un participant.</li>
                  <li><strong>Statut</strong> — un participant peut être Actif (opérationnel),
                    Inactif (désactivé) ou Suspendu (suspendu temporairement).</li>
                  <li><strong>Endpoint</strong> — l'adresse de connexion du participant (URL ou
                    hôte:port). Le type de connexion peut être TCP/IP, HTTP, file de messages
                    ou échange par fichier.</li>
                  <li><strong>Protocoles supportés</strong> — les formats de message que le
                    participant peut échanger (ISO8583, ISO20022).</li>
                  <li><strong>Domestique / Étranger</strong> — le flag <em>isDomestic</em>
                    distingue les participants tunisiens des participants internationaux.
                    Seuls les participants domestiques sont inclus dans le fichier de
                    règlement net envoyé à la BCT.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Ajouter un participant</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouveau participant</strong> (bouton bleu en haut à droite)</li>
                      <li>Saisir le <strong>Nom</strong> du participant (ex: Banque Centrale)</li>
                      <li>Saisir le <strong>Code</strong> unique (ex: BC_TN). Ce code ne pourra plus être modifié</li>
                      <li>Choisir le <strong>Type</strong> : Acquéreur, Émetteur, Switch ou Processeur</li>
                      <li>Choisir le <strong>Statut</strong> initial</li>
                      <li>Saisir l'<strong>URL de l'endpoint</strong> si applicable (ex: banque:8010)</li>
                      <li>Saisir les <strong>Protocoles supportés</strong> séparés par des virgules (ex: ISO8583, ISO20022)</li>
                      <li>Valider avec <strong>Enregistrer</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier un participant</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone crayon de la carte du participant</li>
                      <li>Modifier les champs souhaités (le code n'est pas modifiable)</li>
                      <li>Cliquer sur <strong>Mettre à jour</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer un participant</h4>
                    <p className="text-gray-300 text-xs">
                      Le bouton de suppression n'est pas disponible dans l'interface. La suppression
                      est possible uniquement via l'API. Si un participant n'est plus utilisé,
                      il est recommandé de passer son statut à <strong>Inactif</strong> ou
                      <strong>Suspendu</strong> plutôt que de le supprimer.
                    </p>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>

                <h4 className="font-medium mt-1 mb-1">Types de participant</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Code</th>
                      <th className="p-1 text-left">Libellé</th>
                      <th className="p-1 text-left">Rôle</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">ACQUIRER</td><td className="p-1">Acquéreur</td><td className="p-1">Banque du marchand</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">ISSUER</td><td className="p-1">Émetteur</td><td className="p-1">Banque du porteur</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">SWITCH</td><td className="p-1">Switch</td><td className="p-1">Système central d'interconnexion</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">PROCESSOR</td><td className="p-1">Processeur</td><td className="p-1">Prestataire technique</td></tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Statuts</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Code</th>
                      <th className="p-1 text-left">Libellé</th>
                      <th className="p-1 text-left">Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">ACTIVE</td><td className="p-1">Actif</td><td className="p-1">Le participant peut échanger des transactions</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">INACTIVE</td><td className="p-1">Inactif</td><td className="p-1">Le participant est désactivé</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">SUSPENDED</td><td className="p-1">Suspendu</td><td className="p-1">Suspension temporaire (ex: incident technique)</td></tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Participants pré-initialisés</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Code</th>
                      <th className="p-1 text-left">Nom</th>
                      <th className="p-1 text-left">Type</th>
                      <th className="p-1 text-left">Statut</th>
                      <th className="p-1 text-left">Domestique</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">BANK_A</td><td className="p-1">Bank A - Acquirer</td><td className="p-1">Acquéreur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BANK_B</td><td className="p-1">Bank B - Issuer</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">SWITCH_MAIN</td><td className="p-1">Main Switch</td><td className="p-1">Switch</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">PROC_EU</td><td className="p-1">European Processor</td><td className="p-1">Processeur</td><td className="p-1">Actif</td><td className="p-1">Non</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BANK_C</td><td className="p-1">Bank C - International</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Non</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">SIB</td><td className="p-1">Société Internationale de Banque</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">POSTE_TN</td><td className="p-1">Poste Tunisienne (e-DINAR)</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">ATB</td><td className="p-1">Arab Tunisian Bank</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BH</td><td className="p-1">Banque de l'Habitat</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">BNA</td><td className="p-1">Banque Nationale Agricole</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">UIB</td><td className="p-1">Union Internationale de Banques</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">AMC</td><td className="p-1">Attijari Banque Tunisie</td><td className="p-1">Émetteur</td><td className="p-1">Actif</td><td className="p-1">Oui</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Les participants sont le <strong>socle</strong> du système monétique. Toutes les
                  autres entités (BINs, règles de routage, transactions, compensations) y font
                  référence.
                </p>

                <h4 className="font-medium mt-3 mb-1">Intégration dans le routage</h4>
                <p className="text-gray-300">
                  Les règles de routage établissent des chemins entre participants. Une transaction
                  arrive d'un participant source (acquéreur) et est routée vers un participant
                  de destination (émetteur), en passant éventuellement par d'autres participants.
                </p>

                <h4 className="font-medium mt-3 mb-1">Compensation et règlement</h4>
                <p className="text-gray-300">
                  Lors du clearing, chaque transaction implique deux participants : l'acquéreur
                  et l'émetteur. Les positions nettes sont calculées par participant pour le
                  règlement interbancaire. Les participants marqués comme non domestiques
                  (PROC_EU, BANK_C) sont exclus du fichier de règlement BCT.
                </p>

                <h4 className="font-medium mt-3 mb-1">Contrainte de suppression</h4>
                <p className="text-gray-300">
                  Un participant ne peut pas être supprimé s'il est référencé par d'autres
                  tables (BINs, règles de routage, transactions, marchands, etc.). Il est
                  recommandé de passer son statut à <strong>Inactif</strong> ou
                  <strong>Suspendu</strong> plutôt que de le supprimer.
                </p>

                <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 mt-3">
                  <h4 className="font-medium mb-1">⚠ Flag « Domestique » — impact sur le règlement BCT</h4>
                  <p className="text-xs">
                    Chaque participant possède un indicateur <strong>isDomestic</strong> qui
                    détermine s'il est inclus dans le fichier de règlement net envoyé à la
                    Banque Centrale de Tunisie (BCT).
                  </p>
                  <ul className="list-disc list-inside text-xs mt-1 space-y-1">
                    <li>Un participant marqué <strong>Domestique</strong> (Oui) est inclus
                      dans le calcul des positions nettes et le fichier de règlement BCT.</li>
                    <li>Un participant marqué <strong>Étranger</strong> (Non) est exclu du
                      fichier de règlement BCT. Ses transactions sont réglées hors du
                      circuit BCT.</li>
                    <li><strong>Attention :</strong> marquer par erreur une banque tunisienne
                      comme étrangère l'exclut de la compensation BCT, ce qui empêche son
                      règlement interbancaire.</li>
                  </ul>
                  <p className="text-xs mt-1">
                    Ce champ n'est pas modifiable depuis cette interface. Actuellement,
                    PROC_EU (European Processor) et BANK_C (Bank C - International) sont
                    les seuls marqués comme étrangers.
                  </p>
                </div>

                <h4 className="font-medium mt-3 mb-1">Champs non exposés</h4>
                <p className="text-gray-300">
                  Certains champs techniques ne sont pas affichés dans cette page :
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li><strong>bankCode</strong> — code banque utilisé pour les fichiers de clearing SMT (5 caractères)</li>
                  <li><strong>codeFaconnier</strong> — code façonnier (défaut : 222222)</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la différence entre un Switch et un Processeur ?</dt>
                    <dd className="text-gray-300 mt-1">Le Switch est le système central qui achemine les transactions entre participants. Un Processeur est un prestataire externe qui traite les transactions pour le compte d'un participant (ex: un processeur européen qui gère l'ISO20022 pour le compte d'une banque tunisienne).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Pourquoi ne puis-je pas modifier le code d'un participant ?</dt>
                    <dd className="text-gray-300 mt-1">Le code est l'identifiant unique du participant dans tout le système. Il est utilisé comme référence par les autres tables (règles de routage, BINs, transactions). Modifier le code casserait ces références. Si le code est erroné, il faut recréer le participant.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si je tente de supprimer un participant référencé ailleurs ?</dt>
                    <dd className="text-gray-300 mt-1">La suppression échouera avec une erreur de contrainte de clé étrangère. Il faut d'abord supprimer ou réaffecter toutes les références (BINs, règles de routage, transactions, etc.) avant de pouvoir supprimer le participant.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Combien de participants sont pré-initialisés au démarrage ?</dt>
                    <dd className="text-gray-300 mt-1">12 participants sont créés automatiquement : 5 participants généraux (BANK_A, BANK_B, SWITCH_MAIN, PROC_EU, BANK_C) et 7 banques tunisiennes (SIB, POSTE_TN, ATB, BH, BNA, UIB, AMC).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Qu'est-ce que le flag domestique/étranger ?</dt>
                    <dd className="text-gray-300 mt-1">Le flag <em>isDomestic</em> indique si le participant est une institution tunisienne. Les participants étrangers (PROC_EU, BANK_C) sont exclus du fichier de règlement net envoyé à la BCT. Ce flag ne peut pas être modifié depuis cette interface.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les protocoles supportés sont-ils utilisés par le switch ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Lors de l'envoi d'une transaction vers un participant, le switch utilise le protocole approprié. Par exemple, si le participant ne supporte que l'ISO8583, le switch n'enverra pas de messages ISO20022. Ce champ permet de documenter les capacités techniques de chaque participant.</dd>
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
