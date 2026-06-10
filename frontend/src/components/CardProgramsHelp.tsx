import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const PROGRAM_TYPE_LABELS: Record<string, string> = {
  CONSUMER: 'Particulier',
  CORPORATE: 'Entreprise',
  STUDENT: 'Etudiant',
  PREMIUM: 'Premium',
  PLATINUM: 'Platine',
  SIGNATURE: 'Signature',
  BUSINESS: 'Professionnel',
  CLASSIC: 'Classique',
};

export const PROGRAM_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  ARCHIVED: 'Archive',
};

export const PRODUCT_CARD_TYPE_LABELS: Record<string, string> = {
  DEBIT: 'Debit',
  CREDIT: 'Credit',
  PREPAID: 'Prepaye',
  CHARGE: 'Charge',
  VIRTUAL: 'Virtuelle',
};

export const CARD_NETWORK_LABELS: Record<string, string> = {
  VISA_NET: 'Visa',
  MASTERCARD_NET: 'Mastercard',
  CB_NET: 'Carte Bancaire',
  AMEX_NET: 'American Express',
  VERVE_NET: 'Verve',
};

export function CardProgramsHelp() {
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
              <h2 className="text-lg font-bold">Aide — Programmes & Produits cartes</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. A quoi sert cette page</h3>
                <p>
                  Cette page permet de definir et gerer les <strong>programmes cartes</strong> et
                  les <strong>produits cartes</strong> associes. Un programme carte represente une
                  offre commerciale (ex: « Compte Classic TND »), tandis qu'un produit carte est
                  le type de carte physique ou virtuelle propose dans ce programme (ex: « Carte
                  Debit Classic Visa »).
                </p>
                <ul className="list-disc list-inside mt-1 space-y-1">
                  <li>Consulter les programmes existants et leurs produits</li>
                  <li>Creer et activer/desactiver des programmes</li>
                  <li>Ajouter des produits a un programme avec leurs caracteristiques</li>
                  <li>Configurer les fonctionnalites (sans contact, e-commerce, DAB, etc.)</li>
                  <li>Definir les plafonds par defaut (journalier, hebdomadaire, mensuel, par transaction)</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts clés</h3>
                <ul className="space-y-2">
                  <li><strong>Programme carte</strong> — le conteneur commercial qui regroupe
                    des produits partageant une meme cible (particuliers, entreprises, etudiants,
                    premium). Exemple : « Platinum Rewards ».</li>
                  <li><strong>Produit carte</strong> — le type de carte concret propose dans un
                    programme. Chaque produit a un code unique, un type (Debit, Credit, etc.),
                    un reseau et des caracteristiques specifiques. Exemple : « Classic Debit
                    Visa » avec plafond journalier de 5 000 TND.</li>
                  <li><strong>Reseau</strong> — la marque de la carte (Visa, Mastercard, etc.).
                    Defini au niveau du produit, pas du programme. Un programme peut avoir des
                    produits de differents reseaux.</li>
                  <li><strong>Fonctionnalites</strong> — chaque produit peut activer ou desactiver
                    des capacites : sans contact, paiement en ligne, retrait DAB, utilisation
                    a l'etranger, renouvellement automatique, carte virtuelle.</li>
                  <li><strong>Plafonds par defaut</strong> — limites de montant appliquees aux
                    transactions de ce produit : par transaction, par jour, par semaine,
                    par mois. Ces plafonds peuvent etre modifies au niveau de chaque carte
                    individuelle.</li>
                  <li><strong>Frais annuels</strong> — cout de detention de la carte,
                    facture chaque annee au porteur.</li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Pas à pas</h3>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Creer un programme</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouveau programme</strong> (bouton violet en haut a droite)</li>
                      <li>Saisir le <strong>Nom</strong> du programme (ex: <code>Classic TND</code>)</li>
                      <li>Saisir la <strong>Description</strong> (optionnelle)</li>
                      <li>Choisir le <strong>Type de programme</strong> (Particulier, Entreprise, Premium, etc.)</li>
                      <li>Saisir le <strong>Reseau</strong> associe (ex: <code>VISA</code>, ou laisser vide)</li>
                      <li>Definir la <strong>Date de debut</strong> de validite</li>
                      <li>Definir la <strong>Date de fin</strong> de validite (optionnelle)</li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                    <p className="text-amber-400 mt-1 text-xs">
                      Note : le programme est cree au statut <strong>Brouillon</strong>. Vous devez
                      l'activer pour pouvoir l'utiliser.
                    </p>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Ajouter un produit a un programme</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur <strong>Nouveau produit</strong> (bouton bleu), ou cliquer
                        sur l'icone <strong>+</strong> a cote d'un programme specifique</li>
                      <li>Selectionner le <strong>Programme</strong> dans la liste deroulante</li>
                      <li>Saisir le <strong>Nom du produit</strong> (ex: <code>Classic Debit Visa</code>)</li>
                      <li>Saisir le <strong>Code produit</strong> unique (ex: <code>CLS_DBT_V01</code>)</li>
                      <li>Choisir le <strong>Type</strong> de carte (Debit, Credit, Prepaye, etc.)</li>
                      <li>Choisir le <strong>Reseau</strong> de carte (VISA, MASTERCARD, etc.)</li>
                      <li>Choisir le <strong>Reseau technique</strong> (Visa, Mastercard, CB, etc.)</li>
                      <li>Cocher les <strong>Fonctionnalites</strong> souhaitees</li>
                      <li>Saisir les <strong>Frais annuels</strong> et la <strong>Devise</strong></li>
                      <li>Saisir les <strong>Plafonds par defaut</strong></li>
                      <li>Valider avec <strong>Creer</strong></li>
                    </ol>
                    <p className="text-amber-400 mt-1 text-xs">
                      Note : le produit est cree au statut <strong>Brouillon</strong> par defaut.
                    </p>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Activer / Desactiver un programme ou un produit</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Cliquer sur le bouton d'activation (icone toggle) a cote du statut</li>
                      <li>Le statut passe de <strong>Brouillon</strong> a <strong>Actif</strong>,
                        ou d'<strong>Actif</strong> a <strong>Inactif</strong></li>
                    </ol>
                  </div>
                  <div>
                    <h4 className="font-medium text-indigo-300 mb-1">Supprimer un programme</h4>
                    <ol className="list-decimal list-inside space-y-1 text-gray-300 ml-2">
                      <li>Le bouton suppression n'est pas disponible dans l'interface actuelle</li>
                      <li>La suppression est possible via l'API uniquement</li>
                      <li>Supprimer un programme supprime automatiquement tous ses produits</li>
                    </ol>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Les valeurs</h3>

                <h4 className="font-medium mt-1 mb-1">Types de programme</h4>
                <ul className="space-y-1">
                  <li><strong>Particulier</strong> — programme standard pour les clients particuliers (ex: Classic TND)</li>
                  <li><strong>Entreprise</strong> — programme destine aux entreprises, avec des fonctionnalites adaptees</li>
                  <li><strong>Etudiant</strong> — programme special etudiant (frais reduits, plafonds adaptes)</li>
                  <li><strong>Premium</strong> — programme haut de gamme avec avantages supplementaires</li>
                  <li><strong>Platine</strong> — programme premium avec services exclusifs</li>
                  <li><strong>Signature</strong> — programme elite haut de gamme</li>
                  <li><strong>Professionnel</strong> — programme pour les travailleurs independants et TPE</li>
                  <li><strong>Classique</strong> — programme d'entree de gamme basique</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Types de carte</h4>
                <ul className="space-y-1">
                  <li><strong>Debit</strong> — carte de debit classique liee a un compte courant</li>
                  <li><strong>Credit</strong> — carte de credit avec autorisation de decouvert</li>
                  <li><strong>Prepaye</strong> — carte rechargeable sans compte bancaire</li>
                  <li><strong>Charge</strong> — carte a debit differe (facture mensuelle)</li>
                  <li><strong>Virtuelle</strong> — carte sans support physique, pour le e-commerce</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Reseaux et marques</h4>
                <ul className="space-y-1">
                  <li><strong>Reseau de carte</strong> (marque) — VISA, MASTERCARD, AMEX, CB, VERVE, Autre</li>
                  <li><strong>Reseau technique</strong> — le reseau de traitement : Visa, Mastercard, CB, Amex, Verve</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Fonctionnalites</h4>
                <ul className="space-y-1">
                  <li><strong>Sans contact</strong> — la carte peut etre utilisee sans contact (NFC)</li>
                  <li><strong>En ligne</strong> — les transactions en ligne sont autorisees</li>
                  <li><strong>E-Commerce</strong> — le paiement a distance est autorise</li>
                  <li><strong>DAB</strong> — les retraits aux distributeurs sont autorises</li>
                  <li><strong>International</strong> — la carte est utilisable a l'etranger</li>
                  <li><strong>Renouvelable</strong> — la carte est renouvelee automatiquement a expiration</li>
                  <li><strong>Virtuelle</strong> — une version virtuelle peut etre creee en plus de la carte physique</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Valeurs par defaut (initialisees au demarrage)</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Programme</th>
                      <th className="p-1 text-left">Type</th>
                      <th className="p-1 text-left">Reseau</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">Classic TND Program</td><td className="p-1">Particulier</td><td className="p-1">VISA</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">Premium Business</td><td className="p-1">Entreprise</td><td className="p-1">MASTERCARD</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">Platinum Rewards</td><td className="p-1">Platine</td><td className="p-1">VISA</td></tr>
                  </tbody>
                </table>

                <h4 className="font-medium mt-3 mb-1">Produits associes par defaut</h4>
                <table className="w-full text-xs text-gray-300 mt-1 border border-gray-700">
                  <thead>
                    <tr className="bg-gray-800">
                      <th className="p-1 text-left">Produit</th>
                      <th className="p-1 text-left">Type</th>
                      <th className="p-1 text-left">Reseau</th>
                      <th className="p-1 text-right">Plafond journalier</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-gray-700"><td className="p-1">Classic Debit Visa</td><td className="p-1">Debit</td><td className="p-1">VISA</td><td className="p-1 text-right">5 000 TND</td></tr>
                    <tr className="border-t border-gray-700"><td className="p-1">Prepaid Travel</td><td className="p-1">Prepaye</td><td className="p-1">MASTERCARD</td><td className="p-1 text-right">-</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Fonctionnement</h3>
                <p>
                  Les programmes et produits sont des <strong>entites de configuration</strong>
                  qui definissent les parametres des cartes avant leur creation individuelle.
                </p>
                <ul className="list-disc list-inside space-y-1 text-gray-300 ml-2 mt-1">
                  <li>Un <strong>programme</strong> peut contenir plusieurs <strong>produits</strong>.
                    Les produits apparaissent quand on developpe le programme.</li>
                  <li>Supprimer un programme supprime automatiquement tous ses produits.</li>
                  <li>Les <strong>plafonds</strong> definis sur le produit sont des valeurs
                    par defaut. Ils peuvent etre remplaces sur chaque carte individuelle
                    lors de la creation de la carte (page Emission).</li>
                  <li>Le <strong>statut</strong> controle si le programme ou le produit peut
                    etre utilise : seuls les elements au statut <strong>Actif</strong> sont
                    disponibles pour l'emission de nouvelles cartes.</li>
                  <li>Le <strong>code produit</strong> est unique dans tout le systeme —
                    aucun autre produit ne peut avoir le meme code.</li>
                </ul>

                <h4 className="font-medium mt-3 mb-1">Exemple concrets (d'apres les donnees initiales)</h4>
                <ul className="space-y-2">
                  <li className="text-gray-300">
                    <strong>Classic TND Program (Particulier, VISA)</strong><br />
                    Programme standard pour particuliers, reseau VISA. Contient le produit
                    « Classic Debit Visa » : carte Debit Visa avec plafond journalier de
                    5 000 TND, sans contact et e-commerce actives.
                  </li>
                  <li className="text-gray-300">
                    <strong>Premium Business (Entreprise, MASTERCARD)</strong><br />
                    Programme professionnel pour entreprises, reseau MASTERCARD.
                  </li>
                  <li className="text-gray-300">
                    <strong>Platinum Rewards (Platine, VISA)</strong><br />
                    Programme premium platine, reseau VISA, pour clients haut de gamme.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. FAQ</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre un programme et un produit ?</dt>
                    <dd className="text-gray-300 mt-1">Le programme est l'offre commerciale (ex: « Particulier Classic TND »), tandis que le produit est le type de carte specifique propose dans cette offre (ex: « Carte Debit Classic Visa » avec ses caracteristiques de plafonds, fonctionnalites et frais). Un programme peut contenir plusieurs produits.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Dois-je creer un programme avant un produit ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Le produit doit obligatoirement etre rattache a un programme existant. Vous pouvez creer le produit directement depuis la page en cliquant sur le bouton Nouveau produit, ou le rattacher a un programme specifique en cliquant sur le + a cote du programme souhaite.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Que se passe-t-il si je supprime un programme ?</dt>
                    <dd className="text-gray-300 mt-1">Tous les produits associes a ce programme sont supprimes automatiquement. Cette action est irreversible. Les cartes deja emises ne sont pas affectees.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Les plafonds du produit sont-ils appliques aux cartes emises ?</dt>
                    <dd className="text-gray-300 mt-1">Les plafonds definis sur le produit sont les valeurs par defaut. Lors de la creation d'une carte individuelle (page Emission), ces valeurs sont proposees mais peuvent etre modifiees pour chaque carte. Les plafonds reels sont ceux de la carte, pas ceux du produit.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Puis-je avoir des produits de reseaux differents dans le meme programme ?</dt>
                    <dd className="text-gray-300 mt-1">Oui. Le reseau est defini au niveau du produit, pas au niveau du programme. Vous pouvez donc avoir un produit VISA et un produit MASTERCARD dans le meme programme.</dd>
                  </div>
                  <div>
                    <dt className="font-medium text-amber-300">Quelle est la difference entre le reseau de carte et le reseau technique ?</dt>
                    <dd className="text-gray-300 mt-1">Le reseau de carte (cardBrand) est la marque commerciale visible sur la carte (VISA, MASTERCARD, etc.). Le reseau technique (cardNetwork) est le circuit de traitement des transactions (VISA_NET, MASTERCARD_NET, etc.). Ils sont souvent identiques mais peuvent parfois differer (ex: une carte CB traitee sur le reseau VISA).</dd>
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
