import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

const RECORD_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  CLEARED: 'Compensé',
  DISPUTED: 'En litige',
  REVERSED: 'Reversé',
  SETTLED: 'Réglé',
  FAILED: 'Échoué',
};

const RECON_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  MATCHED: 'Rapproché',
  PARTIALLY_MATCHED: 'Partiellement rapproché',
  DISCREPANCY: 'Écart',
  RESOLVED: 'Résolu',
};

const SRC_LABELS: Record<string, string> = {
  SWITCH: 'Switch',
  PARTICIPANT: 'Participant',
  MERCHANT: 'Commerçant',
  SCHEME: 'Schéma',
};

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 14, fontWeight: 700, margin: '0 0 8px 0', color: '#3b82f6' }}>{title}</h3>
      <div style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--text-secondary)' }}>{children}</div>
    </div>
  );
}

export function ClearingHelp() {
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
                <h2 style={{ fontSize: 18, fontWeight: 700, margin: 0 }}>Aide — Compensation (Clearing)</h2>
                <button onClick={() => setOpen(false)} title="Fermer l'aide" style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: 4 }}>
                  <X size={20} />
                </button>
              </div>

              <Section title="1. À quoi sert cette page">
                <p>Cette page gère la <strong>compensation interbancaire</strong> (clearing). C'est l'étape qui suit les transactions : les banques échangent les données des paiements de la journée, calculent ce que chacune doit aux autres, et préparent les fichiers nécessaires au règlement final.</p>
                <p>Vous pouvez consulter les enregistrements de compensation, voir les positions nettes entre participants, générer des fichiers dans différents formats, importer des fichiers de compensation entrants, et produire des rapports trimestriels.</p>
              </Section>

              <Section title="2. Les concepts à connaître">
                <p><strong>Compensation (clearing) :</strong> après qu'un paiement a été autorisé et traité, les données de la transaction sont envoyées au système de compensation qui les distribue aux banques concernées (l'émetteur et l'acquéreur). C'est ce qui permet de savoir combien chaque banque doit payer ou recevoir.</p>
                <p><strong>Netting (compensation multilatérale) :</strong> au lieu que chaque banque paie toutes les autres une par une, on calcule une position nette pour chaque participant. Si la banque A doit 1000 TND à la banque B et que B doit 600 TND à A, seule la différence de 400 TND est réglée.</p>
                <p><strong>Rapprochement (reconciliation) :</strong> on compare les transactions du switch avec celles déclarées par chaque participant pour détecter les écarts. Les lignes sont « Rapprochées » (MATCHED) si elles correspondent, « Partiellement rapprochées » (PARTIALLY_MATCHED) si certaines divergent.</p>
                <p><strong>Fichier BCT :</strong> le fichier de règlement net destiné à la Banque Centrale de Tunisie. Il récapitule les positions nettes de chaque institution domestique pour le règlement interbancaire.</p>
                <p><strong>Formats de fichier :</strong> plusieurs formats sont disponibles : CSV (tableur), ISO20022 (standard international), COMPCONF (fichier de compensation), CP50 (fichier de règlement). Les formats VISA et MASTERCARD ne sont pas encore disponibles (spécifications propriétaires en attente) — les sélectionner produit une erreur.</p>
              </Section>

              <Section title="3. Utilisation pas à pas">
                <p><strong>Étape 1 : Consulter les enregistrements</strong></p>
                <p>La page affiche automatiquement les enregistrements de compensation du jour dans le tableau de gauche (« Enregistrements »), avec le montant, les frais et le statut de chaque ligne. Les 4 cartes en haut résument les totaux : montant total compensé, frais totaux, montant net, et nombre de litiges en cours.</p>

                <p><strong>Étape 2 : Voir les positions nettes</strong></p>
                <p>Le tableau de droite (« Résultats du netting ») montre pour chaque participant le montant total débité (en rouge), le montant total crédité (en vert), et la position nette (positive = le participant reçoit, négative = il doit payer).</p>

                <p><strong>Étape 3 : Générer un fichier de compensation</strong></p>
                <p>Sélectionnez une date, saisissez l'identifiant (UUID) du participant destinataire, choisissez un format dans la liste (CSV, ISO20022, COMPCONF…), puis cliquez sur « Générer ». Le fichier est téléchargé automatiquement.</p>

                <p><strong>Étape 4 : Importer un fichier de compensation entrant</strong></p>
                <p>Collez le contenu d'un fichier reçu d'un participant dans la zone de texte, sélectionnez le format correspondant, puis cliquez sur « Importer ». Le résultat du rapprochement s'affiche juste en dessous (total de lignes, nombre de rapprochées, nombre de non rapprochées).</p>

                <p><strong>Étape 5 : Télécharger le fichier BCT</strong></p>
                <p>Sélectionnez une date et cliquez sur « Télécharger fichier BCT ». Le fichier CSV des positions nettes pour le règlement à la Banque Centrale est téléchargé.</p>

                <p><strong>Étape 6 : Générer un rapport trimestriel</strong></p>
                <p>Sélectionnez l'année, le trimestre (Q1-Q4) et le schéma de carte (ALL, VISA, MASTERCARD, CB), puis cliquez sur « Générer ». Le rapport s'affiche dans un bloc de texte en bas de la section.</p>
              </Section>

              <Section title="4. Statuts des enregistrements">
                <p>Chaque ligne de compensation a un statut qui indique où elle en est dans le cycle :</p>
                <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse', marginTop: 8 }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)' }}>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Statut</th>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['PENDING', 'En attente', 'La transaction est en cours de traitement.'],
                      ['CLEARED', 'Compensé', 'La transaction a été compensée avec succès.'],
                      ['DISPUTED', 'En litige', 'La transaction est contestée et fait l\'objet d\'un litige.'],
                      ['REVERSED', 'Reversé', 'La transaction a été annulée ou reversée.'],
                      ['SETTLED', 'Réglé', 'Le règlement interbancaire a été effectué.'],
                      ['FAILED', 'Échoué', 'La compensation a échoué (données invalides, participant injoignable).'],
                    ].map(([code, label, desc]) => (
                      <tr key={code} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '6px 8px', fontWeight: 600 }}>{label} ({code})</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{desc}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                <p style={{ marginTop: 12 }}><strong>Rapprochement :</strong></p>
                <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse', marginTop: 8 }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)' }}>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Statut</th>
                      <th style={{ padding: '6px 8px', textAlign: 'left', fontWeight: 600 }}>Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['MATCHED', 'Rapproché', 'La transaction du participant correspond à celle du switch.'],
                      ['PARTIALLY_MATCHED', 'Partiellement rapproché', 'Certaines informations correspondent, d\'autres divergent.'],
                      ['DISCREPANCY', 'Écart', 'Un écart a été détecté entre les données du switch et du participant.'],
                      ['RESOLVED', 'Résolu', 'L\'écart a été traité et résolu.'],
                    ].map(([code, label, desc]) => (
                      <tr key={code} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '6px 8px', fontWeight: 600 }}>{label} ({code})</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{desc}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Section>

              <Section title="5. Rôle de chaque bouton et champ">
                <ul style={{ paddingLeft: 18, margin: 0, lineHeight: 1.9 }}>
                  <li><strong>Section Générer un fichier</strong> — sélectionnez une date, un participant (UUID), un format, puis cliquez sur « Générer » pour télécharger le fichier de compensation.</li>
                  <li><strong>Section Importer un fichier</strong> — collez le contenu d'un fichier entrant, cliquez sur « Importer » pour lancer le rapprochement. Les résultats (total, rapprochées, non rapprochées) s'affichent en vert/rouge.</li>
                  <li><strong>Section Fichier BCT</strong> — sélectionnez une date et cliquez sur « Télécharger » pour obtenir le fichier CSV de règlement net destiné à la Banque Centrale.</li>
                  <li><strong>Section Rapport trimestriel</strong> — choisissez l'année, le trimestre (Q1-Q4) et le schéma de carte (ALL, VISA, MASTERCARD, CB), puis cliquez sur « Générer » pour afficher le rapport.</li>
                  <li><strong>Cartes de résumé</strong> (4 en haut) — affichent les totaux : montant total compensé, frais, net, et nombre de litiges.</li>
                  <li><strong>Tableau des enregistrements</strong> — liste les transactions compensées du jour avec leur montant, frais et statut.</li>
                  <li><strong>Tableau du netting</strong> — montre les positions débit/crédit/net de chaque participant.</li>
                  <li><strong>Tableau d'historique des rapprochements</strong> — liste les 20 derniers rapprochements avec leur date, source, nombre de lignes, rapprochées, non rapprochées et statut.</li>
                </ul>
              </Section>

              <Section title="6. Questions fréquentes">
                <p><strong>Quelle est la différence entre clearing et netting ?</strong></p>
                <p>Le <strong>clearing</strong> (compensation) est l'échange et le rapprochement des données de transaction entre banques. Le <strong>netting</strong> est le calcul des positions nettes : au lieu que chaque banque règle chaque transaction individuellement, on consolide tout en un solde unique par participant.</p>

                <p><strong>Que faire si un enregistrement est en statut DISPUTED ?</strong></p>
                <p>Cela signifie que la transaction est contestée. Rendez-vous sur la page <strong>Litiges</strong> pour gérer la contestation. Tant qu'elle n'est pas résolue, l'enregistrement reste marqué comme en litige ici.</p>

                <p><strong>Qu'est-ce que le fichier BCT ?</strong></p>
                <p>C'est le fichier de règlement net envoyé à la Banque Centrale de Tunisie. Il récapitule les positions nettes de chaque institution domestique pour le règlement interbancaire quotidien.</p>

                <p><strong>Pourquoi les formats VISA et MASTERCARD ne fonctionnent-ils pas ?</strong></p>
                <p>Les formats VISA et MASTERCARD sont propriétaires et leurs spécifications ne sont pas publiques. Ils ne sont pas encore implémentés — les sélectionner produit une erreur. Utilisez CSV, ISO20022, COMPCONF ou CP50 pour un fonctionnement garanti.</p>

                <p><strong>Que signifie un écart (DISCREPANCY) dans le rapprochement ?</strong></p>
                <p>Un écart signifie que le montant ou les données d'une transaction déclarée par un participant ne correspondent pas à celles enregistrées par le switch. Il faut enquêter pour déterminer l'origine de la différence.</p>
              </Section>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export { RECORD_LABELS, RECON_LABELS, SRC_LABELS };
