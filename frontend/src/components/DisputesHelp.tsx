import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

const TRANSITIONS: Record<string, string[]> = {
  OPEN: ['UNDER_REVIEW', 'WITHDRAWN'],
  UNDER_REVIEW: ['EVIDENCE_REQUESTED', 'WON', 'LOST'],
  EVIDENCE_REQUESTED: ['EVIDENCE_SUBMITTED'],
  EVIDENCE_SUBMITTED: ['REPRESENTMENT', 'WON', 'LOST'],
  REPRESENTMENT: ['PRE_ARBITRATION', 'WON', 'LOST'],
  PRE_ARBITRATION: ['ARBITRATION', 'WON', 'LOST'],
  ARBITRATION: ['WON', 'LOST'],
};

const STATUS_DESC: string[] = [
  'Le litige vient d\'être créé. Aucune action n\'a encore été entreprise. Un examen peut être lancé ou le dossier peut être retiré.',
  'Le dossier est analysé par l\'équipe. Des preuves peuvent être demandées, ou une décision (Gagné/Perdu) peut être prise directement.',
  'Des justificatifs sont demandés (généralement au commerçant). Vous devez les fournir via le formulaire « Soumettre une preuve » en bas du détail. Si la date limite est dépassée, le litige passe automatiquement à Perdu.',
  'Les preuves ont été fournies. Le dossier peut maintenant passer en représentation ou être clos par Gagné/Perdu.',
  'Le commerçant re-soumet la transaction avec les justificatifs. Le dossier peut évoluer vers le pré-arbitrage ou être décidé.',
  'Une phase d\'examen approfondi avant l\'arbitrage. Peut aboutir à un arbitrage ou à une décision finale.',
  'Une instance supérieure (schéma de carte, médiateur) va trancher. Issues possibles : Gagné ou Perdu.',
  'Le litige est résolu en faveur de l\'initiateur. Le montant est restitué. État terminal.',
  'Le litige est résolu contre l\'initiateur. Aucun montant n\'est restitué. État terminal.',
  'Le litige a été retiré par l\'initiateur (bouton « Retiré » depuis le statut Ouvert). État terminal.',
];

const EVIDENCE_TYPES = [
  { id: 'RECEIPT', desc: 'Reçu de caisse / ticket de paiement' },
  { id: 'CONTRACT', desc: 'Contrat signé entre les parties' },
  { id: 'COMMUNICATION', desc: 'Échange écrit (email, chat)' },
  { id: 'DELIVERY_PROOF', desc: 'Preuve de livraison (signature, tracking)' },
  { id: 'REFUND_PROOF', desc: 'Preuve de remboursement déjà effectué' },
  { id: 'OTHER_DOCUMENT', desc: 'Autre document justificatif' },
];

const DISPUTE_TYPES = [
  { id: 'FRAUD', desc: 'Transaction frauduleuse (carte volée, usurpation)' },
  { id: 'NOT_RECEIVED', desc: 'Bien ou service non reçu' },
  { id: 'DUPLICATE', desc: 'Transaction dupliquée (débité deux fois)' },
  { id: 'INCORRECT_AMOUNT', desc: 'Montant facturé différent du montant convenu' },
  { id: 'QUALITY_ISSUE', desc: 'Produit défectueux ou non conforme' },
  { id: 'CANCELLED', desc: 'Transaction annulée mais toujours débitée' },
  { id: 'CREDIT_NOT_PROCESSED', desc: 'Remboursement promis mais non effectué' },
  { id: 'OTHER', desc: 'Autre motif de contestation' },
];

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 14, fontWeight: 700, margin: '0 0 8px 0', color: '#3b82f6' }}>{title}</h3>
      <div style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--text-secondary)' }}>{children}</div>
    </div>
  );
}

function SubTitle({ children }: { children: React.ReactNode }) {
  return <h4 style={{ fontSize: 13, fontWeight: 600, margin: '12px 0 6px 0', color: 'var(--text)' }}>{children}</h4>;
}

export function DisputesHelp() {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        title="Aide"
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          width: 32, height: 32, borderRadius: 8,
          border: '1px solid var(--border)', background: 'var(--surface)',
          color: 'var(--text-secondary)', cursor: 'pointer',
          transition: 'border-color 0.15s, color 0.15s',
        }}
        onMouseEnter={e => { e.currentTarget.style.borderColor = '#3b82f6'; e.currentTarget.style.color = '#3b82f6'; }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.color = 'var(--text-secondary)'; }}
      >
        <HelpCircle size={16} />
      </button>

      {open && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.4)', zIndex: 2000,
          display: 'flex', justifyContent: 'flex-end',
        }} onClick={() => setOpen(false)}>
          <div style={{
            width: 520, maxWidth: '100vw', height: '100%', overflowY: 'auto',
            background: 'var(--surface)', borderLeft: '1px solid var(--border)',
            boxShadow: '-4px 0 24px rgba(0,0,0,0.1)',
          }} onClick={e => e.stopPropagation()}>
            <div style={{ padding: '20px 24px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h2 style={{ fontSize: 18, fontWeight: 700, margin: 0 }}>Aide — Litiges & Contestations</h2>
                <button onClick={() => setOpen(false)} title="Fermer l'aide" style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: 4 }}>
                  <X size={20} />
                </button>
              </div>

              <Section title="1. À quoi sert cette page">
                <p>Cette page permet de gérer les litiges (appelés aussi <em>chargebacks</em>). Un litige est une contestation : un porteur, un commerçant ou une banque conteste une transaction et demande son annulation ou un remboursement.</p>
                <p>Vous pouvez créer un nouveau litige, suivre son avancement, soumettre des preuves, et le faire progresser statut par statut jusqu'à sa résolution.</p>
              </Section>

              <Section title="2. Les concepts à connaître">
                <p><strong>Qu'est-ce qu'un litige (chargeback) ?</strong> Quand un porteur ne reconnaît pas une transaction (achat frauduleux, bien non reçu, montant incorrect), il peut la contester via sa banque. Le commerçant peut aussi contester une transaction qu'il estime injustement débitée. Le litige est la procédure qui détermine qui doit être remboursé.</p>

                <SubTitle>Les 4 parties impliquées</SubTitle>
                <ul style={{ paddingLeft: 18, margin: '6px 0', lineHeight: 1.8 }}>
                  <li><strong>Porteur (cardholder)</strong> — le titulaire de la carte bancaire. Il initie souvent le litige.</li>
                  <li><strong>Commerçant (merchant)</strong> — le vendeur qui a accepté le paiement. Il peut défendre la transaction en fournissant des preuves.</li>
                  <li><strong>Émetteur (issuer)</strong> — la banque qui a émis la carte du porteur. Elle représente le porteur.</li>
                  <li><strong>Acquéreur (acquirer)</strong> — la banque du commerçant. Elle représente le commerçant.</li>
                </ul>

                <p><strong>Preuve (evidence) :</strong> un document justificatif (reçu, contrat, preuve de livraison) soumis via le bouton « Soumettre une preuve ».</p>
                <p><strong>Représentation (representment) :</strong> le commerçant re-soumet la transaction contestée accompagnée des preuves, pour tenter de récupérer les fonds.</p>
              </Section>

              <Section title="3. Utilisation pas à pas">
                <p><strong>Étape 1 : Ouvrir un litige</strong></p>
                <p>Cliquez sur le bouton bleu « Ouvrir un litige » en haut à droite. Remplissez le formulaire :</p>
                <ul style={{ paddingLeft: 18, margin: '4px 0', lineHeight: 1.8 }}>
                  <li><strong>Transaction ID</strong> — l'identifiant de la transaction contestée.</li>
                  <li><strong>Type de litige</strong> — le motif (Fraude, Non reçu, Duplicata, Montant incorrect, etc.).</li>
                  <li><strong>Montant + Devise</strong> — le montant contesté et sa devise (TND/EUR/USD).</li>
                  <li><strong>Description du motif</strong> — expliquez pourquoi vous contestez cette transaction.</li>
                  <li><strong>Initié par</strong> — qui lance le litige : Porteur, Commerçant, Émetteur ou Acquéreur.</li>
                </ul>
                <p>Validez avec le bouton « Créer ». Le litige apparaît dans le tableau avec le statut <strong>Ouvert</strong> (OPEN).</p>

                <p><strong>Étape 2 : Consulter un litige</strong></p>
                <p>Cliquez sur une ligne du tableau. Un panneau de détail s'ouvre avec les informations complètes : montant, type, initiateur, dates limites, motif. Vous y trouverez aussi la <strong>chronologie</strong> (timeline) de toutes les actions déjà effectuées.</p>

                <p><strong>Étape 3 : Soumettre une preuve</strong></p>
                <p>Quand le statut est « Preuves demandées » (EVIDENCE_REQUESTED), sélectionnez un type de preuve dans la liste déroulante, ajoutez une description, puis cliquez sur le bouton vert « Soumettre une preuve ». La preuve apparaît dans la liste juste en dessous.</p>
                <p><strong>Attention :</strong> une fois la date limite passée, vous ne pouvez plus soumettre de preuve. Si le litige est en statut « Preuves demandées » (EVIDENCE_REQUESTED) et que le délai expire, le système le passe automatiquement à « Perdu » (LOST). Les autres statuts ne sont pas impactés par cette automaticité.</p>

                <p><strong>Étape 4 : Faire avancer le statut</strong></p>
                <p>Dans le détail du litige, une section « Changer le statut » affiche un bouton par statut atteignable (par exemple : « Examen » pour lancer l'instruction, « Gagné » ou « Perdu » pour clore). Cliquez sur le bouton souhaité. Vous pouvez ajouter une note optionnelle. Le changement est enregistré dans la chronologie.</p>
              </Section>

              <Section title="4. Cycle de vie des statuts">
                <p>Chaque statut autorise des transitions spécifiques. Voici le détail :</p>
                <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse', marginTop: 8 }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)' }}>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Statut</th>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Description</th>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Peut passer vers</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(() => {
                      const allStatuses = [...Object.keys(TRANSITIONS), 'WON', 'LOST', 'WITHDRAWN'];
                      const frLabels: Record<string, string> = { OPEN: 'Ouvert', UNDER_REVIEW: 'Examen', EVIDENCE_REQUESTED: 'Demander preuves', EVIDENCE_SUBMITTED: 'Preuves soumises', REPRESENTMENT: 'Représentation', PRE_ARBITRATION: 'Pré-arbitrage', ARBITRATION: 'Arbitrage', WON: 'Gagné', LOST: 'Perdu', WITHDRAWN: 'Retiré' };
                      const allCodes = [...Object.keys(TRANSITIONS), 'WON', 'LOST', 'WITHDRAWN'];
                      return allCodes.map((s, i) => (
                        <tr key={s} style={{ borderBottom: '1px solid var(--border)' }}>
                          <td style={{ padding: '6px 8px', fontWeight: 600 }}>{frLabels[s]} ({s})</td>
                          <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{STATUS_DESC[i]}</td>
                          <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>
                            {TRANSITIONS[s] ? TRANSITIONS[s].map(t => frLabels[t] || t).join(', ') : '— (terminal)'}
                          </td>
                        </tr>
                      ));
                    })()}
                  </tbody>
                </table>
              </Section>

              <Section title="5. Rôle de chaque bouton">
                <ul style={{ paddingLeft: 18, margin: 0, lineHeight: 1.9 }}>
                  <li><strong>« Ouvrir un litige »</strong> (bleu, en haut à droite) — ouvre le formulaire de création d'un nouveau litige.</li>
                  <li><strong>Onglets Tous / Ouverts / Résolus</strong> — filtrent le tableau : « Tous » liste tout, « Ouverts » montre les litiges en cours (OPEN à ARBITRATION), « Résolus » montre les terminaux (WON, LOST, WITHDRAWN).</li>
                  <li><strong>Ligne du tableau</strong> (clic) — ouvre le détail complet du litige (informations, chronologie, preuves, transitions).</li>
                  <li><strong>« Fermer »</strong> (texte rouge, en haut du détail) — referme le détail et revient à la liste.</li>
                  <li><strong>Boutons de transition</strong> (un par statut atteignable, en haut du détail) — font avancer le litige vers le statut cliqué. Ex. : « Gagné » (WON) pour clore favorablement, « Examen » (UNDER_REVIEW) pour lancer l'instruction.</li>
                  <li><strong>Champ « Notes »</strong> — texte optionnel à associer au changement de statut.</li>
                  <li><strong>« Soumettre une preuve »</strong> (vert) — envoie une nouvelle preuve pour le litige sélectionné.</li>
                </ul>
              </Section>

              <Section title="6. Questions fréquentes">
                <p><strong>Que se passe-t-il si on dépasse la date limite de preuve ?</strong></p>
                <p>Une tâche automatique vérifie chaque jour les délais expirés. Si un litige est en statut « Preuves demandées » (EVIDENCE_REQUESTED) et que la date limite est passée sans soumission de preuve, il passe automatiquement à « Perdu » (LOST) sans intervention manuelle.</p>
                <p><strong>Attention :</strong> cette automaticité ne s'applique qu'au statut EVIDENCE_REQUESTED. Un litige en statut UNDER_REVIEW, REPRESENTMENT ou autre ne sera pas automatiquement perdu si son délai expire.</p>

                <p><strong>Que veut dire WON / LOST ?</strong></p>
                <ul style={{ paddingLeft: 18, margin: '4px 0', lineHeight: 1.7 }}>
                  <li><strong>WON</strong> — le litige est résolu en faveur de l'initiateur. Les fonds sont restitués.</li>
                  <li><strong>LOST</strong> — le litige est résolu contre l'initiateur. Aucun remboursement.</li>
                </ul>
                <p>Ces deux statuts sont <strong>terminaux</strong> : aucune transition n'est possible depuis WON ou LOST.</p>

                <p><strong>Peut-on annuler un litige (WITHDRAWN) ?</strong></p>
                <p>Oui, uniquement depuis le statut OPEN. Cliquez sur le bouton WITHDRAWN dans les transitions possibles. Le litige est retiré et passe en statut terminal « Retiré ».</p>

                <p><strong>Un litige peut-il revenir en arrière ?</strong></p>
                <p>Non, le système n'autorise que des transitions vers l'avant. Les transitions sont définies dans le code et chaque statut n'a qu'un ensemble fixe de statuts atteignables. Vous ne pouvez pas repasser un litige de « En cours d'examen » à « Ouvert ».</p>
              </Section>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
