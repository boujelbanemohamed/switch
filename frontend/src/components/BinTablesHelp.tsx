import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const CARD_BRAND_LABELS: Record<string, string> = {
  VISA: 'Visa',
  MASTERCARD: 'Mastercard',
  AMEX: 'American Express',
  CB: 'Carte Bancaire',
  VERVE: 'Verve',
  OTHER: 'Autre',
};

export const CARD_TYPE_LABELS: Record<string, string> = {
  CREDIT: 'Crédit',
  DEBIT: 'Débit',
  PREPAID: 'Prépayé',
  CHARGE: 'Charge',
  VIRTUAL: 'Virtuelle',
};

export function BinTablesHelp() {
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
              <h2 className="text-lg font-bold">Aide — Tables BIN</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. A quoi sert cette page</h3>
                <p>
                  Le <strong>BIN (Bank Identification Number)</strong> est le prefixe du numero de carte
                  bancaire (6 a 8 premiers chiffres) qui identifie la banque emettrice, le reseau de carte
                  (Visa, Mastercard, etc.), le type de carte et le pays d'emission.
                </p>
                <p className="mt-2">
                  Cette page permet de gerer le repertoire des BIN — la « table BIN » — qui fait le lien
                  entre un prefixe de carte et le participant emetteur. C'est une donnee de reference
                  essentielle pour le routage et la resolution de marque en stand-in.
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter les entrees BIN existantes (prefixe, marque, type, pays, devise, participant)</li>
                  <li>Creer, modifier, activer/desactiver et supprimer des plages BIN</li>
                  <li>Visualiser les participants disponibles pour associer chaque plage BIN</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>BIN</strong> — les 6 a 8 premiers chiffres du PAN. Il identifie de maniere unique
                    l'institution financiere emettrice. Exemple : <code>550000</code> = Mastercard emise par
                    une banque tunisienne.</li>
                  <li><strong>Marque de carte (Card Scheme)</strong> — le reseau de carte : Visa, Mastercard,
                    American Express, Carte Bancaire (CB) ou Autre (OTHER). La marque configuree ici est
                    utilisee par le <strong>stand-in</strong> pour selectionner la regle a appliquer.
                    Si un BIN est parametre avec la mauvaise marque (ex: VISA au lieu de MASTERCARD),
                    le stand-in appliquera une regle prevue pour Visa a une carte Mastercard, ce qui peut
                    donner une autorisation inattendue ou un refus errone. La marque determine aussi les
                    regles de compensation et les frais d'interchange.</li>
                  <li><strong>Type de carte</strong> — la nature du produit : Credit (revolving, paiement differe),
                    Debit (prelevement immediat), Prepaye (chargee a l'avance), Charge (payee integralement
                    chaque mois).</li>
                  <li><strong>Participant</strong> — l'institution financiere a qui appartient cette plage BIN.
                    Chaque plage n'a qu'un seul participant proprietaire.</li>
                  <li><strong>Longueur du BIN</strong> — le nombre de chiffres significatifs du prefixe (4 a 8).
                    Le systeme cherche d'abord une correspondance sur 8 chiffres, puis sur 6.
                    Le champ « longueur declaree » sert a documenter la longueur attendue — la recherche
                    se fait sur la valeur exacte du prefixe, pas sur la longueur declaree. Exemple :
                    si vous saisissez le prefixe <code>550000</code> (6 chiffres) avec une longueur declaree
                    de 8, la recherche sur 6 chiffres trouvera toujours <code>550000</code>.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Creer une entree BIN</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Ajouter un BIN</strong> (bouton bleu en haut a droite)</li>
                      <li>Saisir le <strong>prefixe BIN</strong> (4 a 8 chiffres, ex: <code>123456</code>)</li>
                      <li>Selectionner la <strong>Marque de carte</strong> (Visa, Mastercard, Amex, CB, Other)</li>
                      <li>Choisir le <strong>Participant</strong> emetteur dans la liste deroulante</li>
                      <li>Saisir le <strong>Pays emetteur</strong> (code ISO a 2 lettres, ex: <code>TN</code>)</li>
                      <li>Selectionner le <strong>Type de carte</strong> (Credit, Debit, Prepaye)</li>
                      <li>Saisir la <strong>Devise</strong> (code ISO a 3 lettres, ex: <code>TND</code>)</li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Modifier une entree</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone crayon de la carte ciblee</li>
                      <li>Modifier les champs souhaites</li>
                      <li>Cliquer sur <strong>Sauvegarder</strong> pour valider</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Activer / Desactiver</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone toggle de la carte ciblee</li>
                      <li>Une entree desactivee (affichage grise) n'est pas utilisee pour la resolution BIN</li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer une entree</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur l'icone poubelle de la carte ciblee</li>
                      <li>Confirmer la suppression dans la boite de dialogue</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>
                <h4 className="font-medium mt-1 mb-1">Marques de carte</h4>
                <ul className="space-y-1">
                  <li><span className="bg-[#1a1f71] text-white px-2 py-0.5 rounded text-xs font-medium">Visa</span> — reseau Visa (prefixes commencant par 4)</li>
                  <li><span className="bg-[#eb001b] text-white px-2 py-0.5 rounded text-xs font-medium">Mastercard</span> — reseau Mastercard (prefixes 51-55, 2221-2720)</li>
                  <li><span className="bg-[#2e77bc] text-white px-2 py-0.5 rounded text-xs font-medium">Amex</span> — American Express (prefixes 34, 37)</li>
                  <li><span className="bg-[#0066b3] text-white px-2 py-0.5 rounded text-xs font-medium">CB</span> — Carte Bancaire (reseau interbancaire francais)</li>
                  <li><span className="bg-[#64748b] text-white px-2 py-0.5 rounded text-xs font-medium">Autre</span> — autre reseau non standard</li>
                </ul>
                <h4 className="font-medium mt-3 mb-1">Types de carte</h4>
                <ul className="space-y-1">
                  <li><strong>Crédit</strong> — carte a credit (revolving, paiement differe avec interets eventuels)</li>
                  <li><strong>Débit</strong> — carte de debit (prelevement immediat sur le compte courant)</li>
                  <li><strong>Prépayé</strong> — carte prepayee (chargee a l'avance, pas de decouvert possible)</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Chaque transaction entrante declenche une recherche dans la table BIN pour identifier
                  la marque de carte et le participant emetteur :
                </p>
                <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Le switch extrait les 8 premiers chiffres du PAN et cherche une entree BIN correspondante</li>
                  <li>Si aucune entree trouvee, il recommence avec les 6 premiers chiffres</li>
                  <li>Si une entree est trouvee, sa marque (Visa, Mastercard...) est utilisee pour la suite du traitement</li>
                  <li>Si aucune entree ne correspond, la marque par defaut est « toutes marques » (neutre)</li>
                </ol>
                <p className="mt-2">
                  La marque resolue sert au stand-in pour selectionner la regle appropriee, et au
                  routage pour identifier le participant emetteur.
                  <strong>Si le BIN a une marque incorrecte (Visa au lieu de Mastercard), le stand-in
                  appliquera une regle prevue pour Visa a une transaction Mastercard.</strong>
                  A l'inverse, si le BIN est absent ou desactive, la marque par defaut « toutes marques »
                  sera utilisee — seules les regles stand-in sans restriction de marque pourront matcher.
                </p>
                <p className="mt-2">
                  Chaque plage BIN peut avoir une longueur de 4 a 8 chiffres. La recherche en deux temps
                  (8 puis 6 chiffres) permet de gerer les sous-plages : si une banque possede le bloc
                  <code>550000</code> (6 chiffres) et une autre un sous-bloc plus precis
                  <code>55000012</code> (8 chiffres), le systeme choisit la correspondance la plus
                  specifique (8 chiffres). La longueur declaree dans le champ prevu a cet effet est
                  documentaire : la recherche se fait sur la valeur exacte du prefixe, pas sur la
                  longueur declaree.
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre BIN et IIN ?</dt>
                    <dd className="text-gray-300 mt-1">BIN (Bank Identification Number) et IIN (Issuer Identification Number) designent la meme chose : les premiers chiffres du PAN qui identifient l'emetteur. Le terme BIN est le plus courant.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle longueur de prefixe dois-je saisir ?</dt>
                    <dd className="text-gray-300 mt-1">Le systeme accepte 4 a 8 chiffres. En pratique, 6 chiffres est le standard. Utilisez 8 chiffres si vous devez distinguer des sous-plages specifiques (ex: deux participants differents sous un meme bloc BIN).</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si un numero de carte ne correspond a aucun BIN ?</dt>
                    <dd className="text-gray-300 mt-1">Le systeme utilise la marque par defaut « toutes marques ». Le routage ne pourra pas identifier le participant emetteur — la transaction sera dirigee vers une regle de routage generique ou refusee. Le stand-in ne pourra appliquer qu'une regle sans restriction de marque.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je avoir le meme prefixe BIN pour deux participants differents ?</dt>
                    <dd className="text-gray-300 mt-1">Non. La combinaison (prefixe, longueur declaree, participant) est unique. Si deux banques emettent sous le meme BIN, c'est un cas de portabilite (le PAN change de banque sans changer de numero) — cela se gere au niveau du routage, pas de la table BIN.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Une entree BIN desactivee est-elle ignoree ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Seules les entrees actives sont utilisees pour la resolution. Une entree desactivee apparait en grise sur la page pour rappeler qu'elle existe mais n'est pas prise en compte.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quel est le lien avec le stand-in ?</dt>
                    <dd className="text-gray-300 mt-1">Le stand-in utilise la marque resolue depuis la table BIN pour choisir la regle a appliquer. Avant une recente correction, la marque etait toujours forcee a « Visa », ce qui empechait les regles Mastercard, Amex ou CB de fonctionner. Depuis la correction, la marque resolue ici est utilisee. Vous pouvez donc creer des regles stand-in specifiques par marque (Mastercard, Amex, CB) — a condition que les BIN correspondants soient configures avec la bonne marque.</dd>
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
