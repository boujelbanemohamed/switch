import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const SCHEDULE_TYPE_LABELS: Record<string, string> = {
  INTERCHANGE: 'Interchange',
  SCHEME: 'Reseau',
  PROCESSING: 'Traitement',
  CROSS_BORDER: 'Transfrontalier',
  CURRENCY_CONVERSION: 'Conversion devise',
  ATM: 'Distributeur',
  FIXED: 'Forfaitaire',
  COMPOSITE: 'Compose',
};

export const SCHEDULE_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  ARCHIVED: 'Archive',
};

export const APPLIES_TO_LABELS: Record<string, string> = {
  ALL: 'Tous',
  ISSUER: 'Emetteur',
  ACQUIRER: 'Acquereur',
  MERCHANT: 'Marchand',
  PARTICIPANT: 'Participant',
};

export const CALC_METHOD_LABELS: Record<string, string> = {
  FLAT: 'Montant fixe',
  PERCENTAGE: 'Pourcentage',
  TIERED: 'Palier',
  MIXED: 'Mixte',
  INTERCHANGE_LOOKUP: 'Recherche interchange',
};

export function FeeSchedulesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Baremès de frais</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. A quoi sert cette page</h3>
                <p>
                  Les <strong>barmes de frais</strong> definissent les montants preleves sur les
                  transactions pour couvrir les couts de traitement, les commissions reseau et
                  les frais d'interchange.
                </p>
                <p className="mt-2">
                  Cette page permet de creer et gerer des barmes contenant des regles de calcul.
                  Chaque bar me peut cibler un type de frais (interchange, traitement, etc.),
                  une devise, une periode de validite, et des participants specifiques.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter la liste des barmes existants avec leur statut et priorite</li>
                  <li>Creer, modifier, activer/desactiver et supprimer des barmes</li>
                  <li>Ajouter des regles de calcul detailees dans chaque bar me</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Barme de frais</strong> — un conteneur qui regroupe un ensemble de
                    regles de calcul pour une meme finalite (ex: « Frais de traitement TND »).
                    Chaque bar me a un type, une priorite, une devise et une periode de validite.</li>
                  <li><strong>Priorite du bar me</strong> — plus le chiffre est eleve, plus le
                    bar me est evalue tot (100 avant 10). Les barmes de priorite plus haute
                    sont calcules en premier. Par defaut : 0.</li>
                  <li><strong>Regle de frais</strong> — une instruction de calcul individuelle dans
                    un bar me. Chaque regle a un ordre d'evaluation, une methode de calcul
                    (montant fixe, pourcentage, etc.), un montant/taux, et des filtres
                    optionnels (marque de carte, type de carte, MCC).</li>
                  <li><strong>Ordre de la regle</strong> — plus le chiffre est bas, plus la regle
                    est evaluee tot dans le bar me (ordre 1 avant ordre 2).</li>
                  <li><strong>Filtres</strong> — chaque regle peut etre restreinte a une marque
                    de carte (VISA, MASTERCARD), un type de carte (CREDIT, DEBIT), un code
                    marchand (MCC), une region, ou un montant de transaction minimum/maximum.</li>
                  <li><strong>Periode de validite</strong> — un bar me peut avoir une date de
                    debut et une date de fin optionnelle. Les barmes expirs ne sont pas
                    utilises dans les calculs.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Creer un bar me</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouveau bar me</strong> (bouton bleu en haut a droite)</li>
                      <li>Saisir le <strong>Nom</strong> du bar me (ex: <code>Frais VISA Tunisie</code>)</li>
                      <li>Saisir la <strong>Description</strong> (optionnelle)</li>
                      <li>Choisir le <strong>Type</strong> de bar me (Interchange, Traitement, etc.) — le type
                        sert a classifier le bar me, il n'a pas d'impact sur le calcul</li>
                      <li>Definir la <strong>Priorite</strong> (plus eleve = evalue en premier)</li>
                      <li>Saisir la <strong>Devise</strong> (ex: TND)</li>
                      <li>Choisir la <strong>Date de debut</strong> de validite</li>
                      <li>Choisir la <strong>Date de fin</strong> de validite (optionnelle — laisser vide pour illimite)</li>
                      <li>Choisir la cible <strong>Applicable a</strong> (Tous, Emetteur, Acquereur, Marchand, Participant)</li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                    <p className="text-amber-400 mt-1 text-xs">
                        Note : le bar me est cree au statut <strong>Brouillon</strong>. Vous devez
                        l'activer pour qu'il soit pris en compte dans les calculs.
                    </p>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Ajouter une regle a un bar me</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Selectionner un bar me dans la liste de gauche</li>
                      <li>Cliquer sur <strong>Ajouter regle</strong></li>
                      <li>Saisir le <strong>Nom</strong> de la regle (ex: <code>Frais fixe VISA</code>)</li>
                      <li>Definir l'<strong>Ordre</strong> d'evaluation (plus petit = evalue en premier)</li>
                      <li>Choisir la <strong>Methode</strong> de calcul</li>
                      <li>Saisir le <strong>Montant fixe</strong> et/ou le <strong>Taux</strong>
                        selon la methode choisie</li>
                      <li>Definir le <strong>Montant minimum</strong> et <strong>maximum</strong>
                        (optionnels — le resultat du calcul sera plaque a ces bornes)</li>
                      <li>Ajouter des <strong>Filtres</strong> optionnels (Reseau, Type de carte, MCC)</li>
                      <li>Valider avec <strong>Ajouter regle</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Activer / Desactiver un bar me</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Selectionner le bar me dans la liste de gauche</li>
                      <li>Cliquer sur le bouton <strong>Activer</strong> ou <strong>Desactiver</strong></li>
                      <li>Un bar me desactive n'est pas inclus dans les calculs de frais</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer une regle</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Dans le tableau des regles, cliquer sur l'icone poubelle de la regle cible</li>
                      <li>La regle est supprimee immediatement</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>
                <h4 className="font-medium mt-1 mb-1">Types de bar me (classification)</h4>
                <ul className="space-y-1">
                  <li><strong>Interchange</strong> — frais d'interchange entre banques emettrices et acquereurs</li>
                  <li><strong>Reseau</strong> — frais de reseau (VISA, Mastercard, etc.)</li>
                  <li><strong>Traitement</strong> — frais de traitement de la plateforme</li>
                  <li><strong>Transfrontalier</strong> — frais pour transactions internationales</li>
                  <li><strong>Conversion devise</strong> — frais de conversion de change</li>
                  <li><strong>Distributeur</strong> — frais de retrait ATM</li>
                  <li><strong>Forfaitaire</strong> — frais forfaitaires independants du montant</li>
                  <li><strong>Compose</strong> — bar me combinant plusieurs types de frais</li>
                </ul>
                <p className="text-gray-400 text-xs mt-1">Le type est informatif et n'affecte pas le calcul.</p>

                <h4 className="font-medium mt-3 mb-1">Statuts</h4>
                <ul className="space-y-1">
                  <li><span className="text-yellow-400 font-medium">Brouillon</span> — le bar me est en cours de configuration, pas encore utilisable</li>
                  <li><span className="text-green-500 font-medium">Actif</span> — le bar me est pris en compte par le moteur de calcul</li>
                  <li><span className="text-gray-400 font-medium">Inactif</span> — le bar me existe mais n'est pas evalue</li>
                  <li><span className="text-red-400 font-medium">Archive</span> — le bar me n'est plus utilise (historique)</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Methodes de calcul</h4>
                <ul className="space-y-3">
                  <li>
                    <span className="font-medium">Montant fixe</span> — preleve un montant forfaitaire
                    quelle que soit la transaction.<br />
                    <span className="text-gray-400">Formule :</span> frais = montant fixe
                  </li>
                  <li>
                    <span className="font-medium">Pourcentage</span> — preleve un pourcentage du
                    montant de la transaction, plaque entre min et max.<br />
                    <span className="text-gray-400">Formule :</span> frais = montant x taux / 100,
                    plaque a [min, max]
                  </li>
                  <li>
                    <span className="font-medium">Mixte</span> — combine un montant fixe et un
                    pourcentage, plaque entre min et max.<br />
                    <span className="text-gray-400">Formule :</span> frais = montant fixe + (montant x taux / 100),
                    plaque a [min, max]
                  </li>
                  <li>
                    <span className="font-medium">Palier</span> — meme formule que Mixte (fixe +
                    pourcentage, plaque entre min et max). Le nom « Palier » permet de distinguer
                    semantiquement un tarif progressif d'un tarif mixte, mais la formule est identique.<br />
                    <span className="text-gray-400">Formule :</span> identique a Mixte
                  </li>
                  <li>
                    <span className="font-medium">Recherche interchange</span> — utilise un montant
                    d'interchange predetermine.<br />
                    <span className="text-amber-400 text-xs">Note importante :</span> cette methode
                    n'est <strong>pas connectee</strong> a la table d'interchange de la page
                    « Frais d'interchange ». Le montant est toujours 0 en l'etat actuel
                    (non integre au flux).<br />
                    <span className="text-gray-400">Formule :</span> frais = 0
                  </li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Filtres optionnels</h4>
                <ul className="space-y-1">
                  <li><strong>Reseau</strong> — marque de carte (VISA, MASTERCARD, etc.). La regle ne s'applique qu'a ce reseau.</li>
                  <li><strong>Type de carte</strong> — CREDIT, DEBIT, etc. La regle ne s'applique qu'a ce type.</li>
                  <li><strong>MCC</strong> — code marchand a 4 chiffres. Ex: 5812 pour restauration.</li>
                  <li><strong>Montant min/max de transaction</strong> — la regle ne s'applique que si le montant est dans cet intervalle.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Le calcul des frais fonctionne en deux niveaux :
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li><strong>Selection des barmes actifs</strong> — tous les barmes au statut
                    Actif, dont la date de validite inclut la date du jour, sont charges et
                    tries par priorite <strong>decroissante</strong> (priorite 100 avant 10)</li>
                  <li><strong>Evaluation des regles</strong> — pour chaque bar me, les regles sont
                    evaluees dans l'ordre croissant (ordre 1 avant ordre 2). Seules les regles
                    dont tous les filtres correspondent sont appliquees. Le montant total du bar me
                    est la somme des regles qui ont match e</li>
                </ol>
                <p className="mt-2">
                  Les frais de tous les barmes sont ensuite additionnes pour donner le frais total
                  de la transaction.
                </p>
                <p className="mt-2">
                  <strong>Exemple de calcul (Pourcentage avec plafond) :</strong>
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300 ml-2">
                  <li>Regle : taux = 1,5%, min = 0,50 TND, max = 15 TND</li>
                  <li>Transaction de 200 TND : 200 x 1,5 / 100 = 3,00 TND. Le resultat (3,00) est
                    entre min (0,50) et max (15) → frais = 3,00 TND</li>
                  <li>Transaction de 20 TND : 20 x 1,5 / 100 = 0,30 TND. En dessous du min (0,50) → frais = 0,50 TND</li>
                  <li>Transaction de 2 000 TND : 2 000 x 1,5 / 100 = 30 TND. Au-dessus du max (15) → frais = 15 TND</li>
                </ul>
                <p className="mt-2">
                  <strong>Important :</strong> le moteur de calcul est actuellement un service
                  autonome accessible via API. Les frais calcules ne sont pas encore
                  automatiquement appliques aux transactions dans le flux de traitement.
                  L'integration au cycle de vie des transactions est prevue dans une phase
                  ulterieure.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre un bar me et une regle ?</dt>
                    <dd className="text-gray-300 mt-1">Le bar me est le conteneur (ex: « Frais VISA 2024 »). La regle est l'instruction de calcul individuelle a l'interieur du bar me (ex: « 1,5% du montant, min 0,50 TND, max 15 TND »). Un bar me peut avoir plusieurs regles.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Pourquoi la priorite des barmes est-elle inversee par rapport aux regles de routage ?</dt>
                    <dd className="text-gray-300 mt-1">Dans le routage, priorite 1 = premier evalue (ordre croissant). Dans les barmes, c'est l'inverse : priorite 100 = premier evalue (ordre decroissant). Cela permet de donner une priorite « haute » (ex: 1000) a un bar me qui doit primer sur les autres. Les deux conventions coexistent dans le switch.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les frais sont-ils automatiquement deduits des transactions ?</dt>
                    <dd className="text-gray-300 mt-1">Non, pas encore. Le moteur de calcul est fonctionnel et peut etre interroge via l'API, mais les frais ne sont pas encore automatiques dans le flux de transaction. L'API de calcul permet de simuler et valider les montants.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si une transaction correspond a plusieurs regles dans le meme bar me ?</dt>
                    <dd className="text-gray-300 mt-1">Toutes les regles qui matchent sont appliquees et additionnees. Ce n'est pas un systeme de « premiere regle gagnante » — chaque regle contribue au total du bar me.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Comment faire pour qu'un bar me ne s'applique qu'a un participant specifique ?</dt>
                    <dd className="text-gray-300 mt-1">Utilisez le champ « Applicable a » pour choisir le type de cible (Emetteur, Acquereur, Marchand, Participant). Vous pouvez aussi filtrer par marque de carte et type de carte au niveau de chaque regle.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je avoir deux barmes actifs pour la meme periode ?</dt>
                    <dd className="text-gray-300 mt-1">Oui, ils seront additionnes. Le systeme est cumulatif : tous les barmes actifs et valides sont evalues et leurs frais s'additionnent. La priorite determine seulement l'ordre d'evaluation, pas l'exclusivite.</dd>
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
