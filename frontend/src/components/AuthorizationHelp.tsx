import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const RULE_TYPE_LABELS: Record<string, string> = {
  SOLDE: 'Solde',
  LIMIT: 'Plafond',
  VELOCITY: 'Vélocité',
  FRAUD: 'Fraude',
  RISK: 'Risque',
  MERCHANT: 'Commerçant',
  PRODUCT: 'Produit',
  GEO: 'Géographique',
  TIME: 'Horaire',
  CUSTOM: 'Personnalisé',
};

export const RULE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  INACTIVE: 'Inactif',
  TEST: 'Test',
};

export const ACTION_LABELS: Record<string, string> = {
  APPROVE: 'Approuver',
  DECLINE: 'Refuser',
  CHALLENGE: 'Défi (3DS)',
  REVIEW: 'Révision',
  TFA: '2FA',
  PIN: 'PIN',
};

export const DECISION_LABELS: Record<string, string> = {
  APPROVED: 'Approuvée',
  DECLINED: 'Refusée',
  CHALLENGED: 'Défiée',
  REVIEW: 'Révision',
  TIMEOUT: 'Délai dépassé',
  ERROR: 'Erreur',
};

export const HOLD_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Réservation active',
  RELEASED: 'Libérée',
  CAPTURED: 'Capturée',
  EXPIRED: 'Expirée',
};

export function AuthorizationHelp() {
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
              <h2 className="text-lg font-bold">Aide — Autorisation</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page gère le <strong>moteur d'autorisation</strong> du switch. Son rôle est
                  de décider en temps réel s'il faut <strong>approuver ou refuser</strong> une
                  transaction selon des règles paramétrables. Elle permet aussi de simuler une
                  autorisation, de consulter les décisions passées et de gérer les réservations
                  de fonds (holds).
                </p>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>
                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Autorisation :</strong> demande en temps réel (online) adressée au
                    switch pour vérifier si une transaction peut être acceptée. Le switch applique
                    ses règles et répond <em>Approuvée</em>, <em>Refusée</em> ou <em>Défiée</em>
                    (authentification 3DS).
                  </li>
                  <li>
                    <strong>Règle d'autorisation :</strong> condition qui déclenche une action
                    (approuver, refuser, défier). Les règles sont évaluées par ordre de priorité.
                    Exemples : « refuser si montant &gt; 10 000 TND », « défier si pays ≠ Tunisie ».
                  </li>
                  <li>
                    <strong>Hold (réservation) :</strong> blocage temporaire d'un montant sur le
                    compte du porteur lorsqu'une transaction est autorisée. Le hold est libéré si
                    la transaction est annulée ou capturé (converti en débit réel) si elle est
                    finalisée.
                  </li>
                  <li>
                    <strong>Plafond (LIMIT) :</strong> règle qui fixe un montant maximal autorisé
                    par transaction, par jour ou par période.
                  </li>
                  <li>
                    <strong>Vélocité (VELOCITY) :</strong> règle qui limite le nombre de
                    transactions dans une fenêtre de temps (ex. max 3 transactions en 5 minutes).
                  </li>
                  <li>
                    <strong>Condition :</strong> un test sur un champ de la transaction (montant,
                    devise, pays, type de carte, canal…) combiné à un opérateur (égal, supérieur,
                    compris entre, dans une liste…).
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>Créer une règle :</strong> dans l'onglet Règles, cliquez sur
                    « Ajouter une Règle ». Donnez-lui un nom, choisissez le type
                    (Plafond, Vélocité, Fraude…), l'action (Approuver/Refuser/Défi), la priorité
                    (plus le chiffre est petit, plus la règle est évaluée tôt)
                    et le statut.
                  </li>
                  <li>
                    <strong>Ajouter des conditions :</strong> dans la modale, cliquez sur
                    « Ajouter une Condition ». Sélectionnez un champ (Montant, Devise, Pays…),
                    un opérateur (=, &gt;, &lt;, Entre, Dans…) et une valeur. Toutes les conditions
                    sont combinées avec un ET logique.
                  </li>
                  <li>
                    <strong>Modifier / Activer / Désactiver :</strong> utilisez les boutons
                    « Modifier », « Activer » ou « Désactiver » dans la colonne Actions du
                    tableau. Désactiver une règle l'exclut de l'évaluation sans la supprimer.
                  </li>
                  <li>
                    <strong>Simuler une autorisation :</strong> dans l'onglet Simulateur,
                    saisissez un numéro de carte, un montant et un ID commerçant, puis cliquez
                    sur « Simuler ». Le résultat affiche la décision et le code réponse.
                  </li>
                  <li>
                    <strong>Consulter les décisions :</strong> dans l'onglet Décisions, saisissez
                    l'UUID d'une carte et cliquez sur Load pour voir l'historique des décisions
                    d'autorisation.
                  </li>
                  <li>
                    <strong>Gérer les holds :</strong> dans l'onglet Blocages, saisissez l'UUID
                    d'une carte et cliquez sur Load. Les holds actifs peuvent être libérés
                    (annulation de la réservation) ou capturés (conversion en débit).
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts et cycle de vie</h3>

                <p className="font-semibold mb-1">Règle d'autorisation</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Actif</span></td><td className="p-2 border border-gray-700">La règle est évaluée lors des autorisations.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Inactif</span></td><td className="p-2 border border-gray-700">Règle désactivée, non évaluée.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Test</span></td><td className="p-2 border border-gray-700">Règle en mode test : évaluée mais n'affecte pas la décision finale.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Blocage (Hold)</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Réservation active</span></td><td className="p-2 border border-gray-700">Montant bloqué sur le compte du porteur. Peut être libéré ou capturé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Libérée</span></td><td className="p-2 border border-gray-700">Réservation annulée, le montant est débloqué.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-purple-900/50 text-purple-400">Capturée</span></td><td className="p-2 border border-gray-700">Réservation convertie en débit réel. Le montant est prélevé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Expirée</span></td><td className="p-2 border border-gray-700">La réservation a expiré automatiquement (délai dépassé). Le montant est débloqué.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Décision d'autorisation</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Décision</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Approuvée</span></td><td className="p-2 border border-gray-700">Transaction autorisée. Un hold est placé sur le compte.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Refusée</span></td><td className="p-2 border border-gray-700">Transaction refusée. Aucun hold n'est placé.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-purple-900/50 text-purple-400">Défiée</span></td><td className="p-2 border border-gray-700">Authentification 3DS demandée. Le porteur doit se vérifier.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Révision</span></td><td className="p-2 border border-gray-700">Transaction marquée pour révision manuelle.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Délai dépassé</span></td><td className="p-2 border border-gray-700">L'autorisation n'a pas abouti dans le temps imparti.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Erreur</span></td><td className="p-2 border border-gray-700">Erreur système lors du traitement de l'autorisation.</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Rôle de chaque bouton et champ</h3>
                <ul className="space-y-2">
                  <li>
                    <strong>4 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>Règles</em> — liste des règles d'autorisation avec leurs conditions.</li>
                      <li><em>Décisions</em> — historique des décisions pour une carte.</li>
                      <li><em>Blocages</em> — réservations de fonds actives et passées.</li>
                      <li><em>Simulateur</em> — test d'une autorisation sans transaction réelle.</li>
                    </ul>
                  </li>
                  <li>
                    <strong>« Ajouter une Règle »</strong> : ouvre une modale pour créer une
                    nouvelle règle (nom, type, action, priorité, statut, conditions, restrictions
                    horaires).
                  </li>
                  <li>
                    <strong>Type de règle</strong> : Plafond (montant max), Vélocité (fréquence),
                    Fraude (détection), Risque (scoring), Commerçant (par MCC), Produit (type de
                    carte), Géographique (pays), Horaire (plage), Solde, Personnalisé.
                  </li>
                  <li>
                    <strong>Action</strong> : Approuver, Refuser, Défi (3DS), Révision, 2FA, PIN.
                  </li>
                  <li>
                    <strong>Priorité</strong> : ordre d'évaluation des règles. Plus le chiffre est
                    petit, plus la règle est évaluée tôt. La priorité 1 est la première évaluée.
                  </li>
                  <li>
                    <strong>Conditions</strong> : combinaison de critères (montant, devise, canal…)
                    avec des opérateurs (=, ≠, &gt;, &lt;, Entre, Dans). Toutes les conditions
                    doivent être vraies pour que la règle s'applique (ET logique).
                  </li>
                  <li>
                    <strong>« Modifier »</strong> : édite une règle existante.
                  </li>
                  <li>
                    <strong>« Activer » / « Désactiver »</strong> : bascule le statut d'une règle
                    sans la supprimer.
                  </li>
                  <li>
                    <strong>« Supprimer »</strong> : supprime définitivement une règle.
                  </li>
                  <li>
                    <strong>« Libérer » (hold)</strong> : annule une réservation de fonds sans
                    débiter le compte.
                  </li>
                  <li>
                    <strong>« Capturer » (hold)</strong> : convertit la réservation en débit réel
                    sur le compte.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Quelle est la différence entre une règle et une condition ?</strong></p>
                  <p>
                    Une <strong>règle</strong> est un ensemble complet : un déclencheur (type),
                    une action à prendre (approuver/refuser), une priorité et un statut.
                    Une <strong>condition</strong> est un test individuel à l'intérieur d'une
                    règle (ex. « montant &gt; 1000 »). Une règle peut avoir plusieurs conditions,
                    toutes reliées par ET.
                  </p>

                  <p><strong>Comment sont évaluées les règles ?</strong></p>
                  <p>
                    Les règles sont triées par priorité <strong>croissante</strong> (priorité 1
                    évaluée en premier). Le switch parcourt les règles actives dont les conditions
                    sont vraies dans cet ordre. Si une règle dit <strong>Refuser</strong> ou
                    <strong>Défi</strong>, l'évaluation s'arrête et cette décision est appliquée.
                    Si une règle dit <strong>Approuver</strong>, le switch continue d'évaluer
                    les règles suivantes (une règle Approuver suivie d'une règle Refuser sera
                    refusée). Après les règles, les plafonds et la vélocité sont aussi vérifiés.
                  </p>

                  <p><strong>Que se passe-t-il si aucune règle ne matche ?</strong></p>
                  <p>
                    Si aucune règle active ne correspond à la transaction, la demande est
                    transmise à l'émetteur pour décision (ou refusée selon la configuration).
                  </p>

                  <p><strong>Combien de temps dure un hold ?</strong></p>
                  <p>
                    La durée par défaut est de 30 minutes. Si la transaction n'est ni capturée
                    ni libérée dans ce délai, le hold expire automatiquement et le montant est
                    débloqué.
                  </p>

                  <p><strong>Qu'est-ce que le mode Test sur une règle ?</strong></p>
                  <p>
                    Le statut « Test » permet d'évaluer une règle sans appliquer son action.
                    Utile pour valider une nouvelle règle avant de l'activer en production.
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
