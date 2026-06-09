import { useState } from 'react';
import { HelpCircle, X } from 'lucide-react';

export const ACS_AUTH_STATUS_LABELS: Record<string, string> = {
  CREATED: 'Créée',
  CHALLENGE_REQUIRED: 'Défi requis',
  AUTHENTICATED: 'Authentifiée',
  FAILED: 'Échouée',
  DECLINED: 'Refusée',
  EXPIRED: 'Expirée',
};

export const ACS_ENROLLMENT_STATUS_LABELS: Record<string, string> = {
  ENROLLED: 'Inscrite',
  ACTIVE: 'Active',
  SUSPENDED: 'Suspendue',
  CANCELLED: 'Annulée',
};

export const EPG_TXN_STATUS_LABELS: Record<string, string> = {
  INITIATED: 'Initié',
  AUTHENTICATED: 'Authentifié',
  AUTHORIZED: 'Autorisé',
  CAPTURED: 'Capturé',
  FAILED: 'Échoué',
  REFUNDED: 'Remboursé',
  CHARGEBACK: 'Contesté',
  CANCELED: 'Annulé',
};

export const THREEDS_SESSION_STATUS_LABELS: Record<string, string> = {
  CREATED: 'Créée',
  AUTH_REQ_SENT: 'Requête AReq envoyée',
  AUTH_REQ_RECEIVED: 'Requête AReq reçue',
  CHALLENGE_SENT: 'Défi envoyé',
  CHALLENGE_RECEIVED: 'Défi reçu',
  COMPLETED: 'Terminée',
  ERROR: 'Erreur',
  TIMEOUT: 'Délai expiré',
};

export const EPG_MERCHANT_STATUS_LABEL = (isActive: boolean) => isActive ? 'Actif' : 'Inactif';

export function EcommerceHelp() {
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
              <h2 className="text-lg font-bold">Aide — E-Commerce</h2>
              <button onClick={() => setOpen(false)} className="p-1 hover:bg-gray-700 rounded transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 text-sm leading-relaxed">
              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">1. À quoi sert cette page</h3>
                <p>
                  Cette page regroupe les trois composants du paiement en ligne sécurisé&nbsp;:
                </p>
                <ul className="list-disc list-inside mt-2 space-y-1">
                  <li>
                    <strong>ACS (Access Control Server)</strong> — authentifie le porteur de carte
                    via le protocole 3-D Secure. C'est le serveur de l'émetteur qui valide
                    l'identité du client lors d'un paiement en ligne.
                  </li>
                  <li>
                    <strong>EPG (E-Payment Gateway)</strong> — passerelle de paiement qui initie
                    et suit les transactions pour les commerçants en ligne.
                  </li>
                  <li>
                    <strong>3DSS (3DS Server)</strong> — serveur qui orchestre le flux
                    d'authentification 3-D Secure entre le marchand, l'ACS et l'émetteur.
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">2. Les concepts à connaître</h3>

                <p className="mb-2">
                  <strong>3-D Secure (3DS) :</strong> protocole de sécurité qui ajoute une étape
                  d'authentification du porteur lors d'un paiement en ligne. Quand vous achetez
                  sur un site et que le navigateur vous redirige vers une page de votre banque
                  pour saisir un code reçu par SMS, c'est le 3-D Secure qui s'exécute.
                </p>

                <ul className="list-disc list-inside space-y-1">
                  <li>
                    <strong>Challenge :</strong> l'étape où le porteur doit prouver son identité
                    (par code SMS, OTP, email, notification d'application, biométrie ou mot de
                    passe). Sans challenge réussi, la transaction peut être refusée.
                  </li>
                  <li>
                    <strong>ACS (Access Control Server) :</strong> serveur de l'émetteur qui
                    reçoit la demande d'authentification, décide si un challenge est nécessaire,
                    et renvoie la réponse (authentifiée, refusée, ou défi requis).
                  </li>
                  <li>
                    <strong>EPG (E-Payment Gateway) :</strong> passerelle qui enregistre les
                    transactions des commerçants en ligne. Elle peut être liée à l'ACS pour
                    déclencher le 3-D Secure automatiquement.
                  </li>
                  <li>
                    <strong>3DSS (3DS Server) :</strong> intermédiaire technique qui construit
                    les messages 3-D Secure (AReq, CReq) et les achemine entre le marchand,
                    l'ACS et l'émetteur via le DS (Directory Server).
                  </li>
                  <li>
                    <strong>Enrôlement (Enrollment) :</strong> enregistrement d'une carte auprès
                    de l'ACS pour qu'elle puisse être authentifiée en 3-D Secure.
                  </li>
                  <li>
                    <strong>ECI (Electronic Commerce Indicator) :</strong> code qui indique le
                    niveau de sécurité de l'authentification (ex. 05 = authentifié, 06 =
                    authentification tentée ou non réalisée).
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">3. Utilisation pas à pas</h3>
                <ol className="list-decimal list-inside space-y-2">
                  <li>
                    <strong>ACS — Créer une authentification :</strong> dans l'onglet ACS,
                    remplissez l'ID de transaction, le montant et la devise, puis cliquez sur
                    «&nbsp;Create Authentication&nbsp;». L'authentification est créée avec le
                    statut «&nbsp;Créée&nbsp;».
                  </li>
                  <li>
                    <strong>ACS — Demander un challenge :</strong> après création, cliquez sur
                    «&nbsp;Request Challenge&nbsp;» pour déclencher le défi 3-D Secure. Le
                    statut passe à «&nbsp;Défi requis&nbsp;».
                  </li>
                  <li>
                    <strong>ACS — Enrôler une carte :</strong> dans la section «&nbsp;Card
                    Enrollments&nbsp;», cliquez sur «&nbsp;Enroll Card&nbsp;», saisissez l'ID
                    de la carte, puis «&nbsp;Enroll&nbsp;».
                  </li>
                  <li>
                    <strong>EPG — Initier un paiement :</strong> dans l'onglet EPG, saisissez
                    l'ID du commerçant (merchantId), un ID de transaction, un montant et une
                    devise, puis «&nbsp;Initiate Payment&nbsp;».
                  </li>
                  <li>
                    <strong>EPG — Configurer un commerçant :</strong> dans la section «&nbsp;Merchant
                    Configurations&nbsp;», cliquez sur «&nbsp;Add Merchant&nbsp;», remplissez
                    les champs et générez des clés.
                  </li>
                  <li>
                    <strong>3DSS — Créer une session :</strong> dans l'onglet 3DSS, saisissez
                    un ID de transaction et une URL de notification, puis «&nbsp;Create 3DS
                    Session&nbsp;». La session est créée avec le statut «&nbsp;Créée&nbsp;».
                  </li>
                  <li>
                    <strong>3DSS — Annuler une session :</strong> dans la section «&nbsp;Active
                    Sessions&nbsp;», cliquez sur le bouton rouge «&nbsp;Cancel&nbsp;» pour une
                    session qui n'est pas encore terminée.
                  </li>
                </ol>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">4. Statuts et cycle de vie</h3>

                <p className="font-semibold mb-1">Authentification ACS</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Créée</span></td><td className="p-2 border border-gray-700">Authentification créée, en attente d'action.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Défi requis</span></td><td className="p-2 border border-gray-700">Un challenge a été demandé pour vérifier le porteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Authentifiée</span></td><td className="p-2 border border-gray-700">Le porteur a été authentifié avec succès.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Échouée</span></td><td className="p-2 border border-gray-700">L'authentification a échoué pour une raison technique.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Refusée</span></td><td className="p-2 border border-gray-700">L'authentification a été refusée par l'émetteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Expirée</span></td><td className="p-2 border border-gray-700">L'authentification n'a pas été complétée à temps.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Session 3DS</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Créée</span></td><td className="p-2 border border-gray-700">Session créée, en attente d'envoi de la requête AReq.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Requête AReq envoyée</span></td><td className="p-2 border border-gray-700">Requête d'authentification envoyée au DS.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-yellow-900/50 text-yellow-400">Requête AReq reçue</span></td><td className="p-2 border border-gray-700">Réponse d'authentification reçue du DS.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-indigo-900/50 text-indigo-400">Défi envoyé</span></td><td className="p-2 border border-gray-700">Challenge CReq envoyé au porteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-indigo-900/50 text-indigo-400">Défi reçu</span></td><td className="p-2 border border-gray-700">Réponse CRes reçue du porteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Terminée</span></td><td className="p-2 border border-gray-700">Session terminée avec succès.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Erreur</span></td><td className="p-2 border border-gray-700">Une erreur est survenue pendant le flux 3DS.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Délai expiré</span></td><td className="p-2 border border-gray-700">Session annulée ou expirée (timeout).</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Transaction EPG</p>
                <table className="w-full text-xs border-collapse mb-4">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Initié</span></td><td className="p-2 border border-gray-700">Transaction créée, en attente de traitement.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Autorisé</span></td><td className="p-2 border border-gray-700">Transaction autorisée par l'émetteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Capturé</span></td><td className="p-2 border border-gray-700">Transaction capturée, fonds transférés.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-red-900/50 text-red-400">Échoué</span></td><td className="p-2 border border-gray-700">Transaction en échec.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-purple-900/50 text-purple-400">Remboursé</span></td><td className="p-2 border border-gray-700">Transaction remboursée au porteur.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Contesté</span></td><td className="p-2 border border-gray-700">Transaction contestée par le porteur (chargeback).</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Annulé</span></td><td className="p-2 border border-gray-700">Transaction annulée.</td></tr>
                  </tbody>
                </table>

                <p className="font-semibold mb-1">Enrôlement ACS</p>
                <table className="w-full text-xs border-collapse">
                  <thead>
                    <tr className="bg-gray-800 text-gray-400">
                      <th className="text-left p-2 border border-gray-700">Statut</th>
                      <th className="text-left p-2 border border-gray-700">Signification</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-400">Inscrite</span></td><td className="p-2 border border-gray-700">Carte enrôlée, prête pour l'authentification.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-green-900/50 text-green-400">Active</span></td><td className="p-2 border border-gray-700">Enrôlement actif, l'authentification 3DS est possible.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-orange-900/50 text-orange-400">Suspendue</span></td><td className="p-2 border border-gray-700">Enrôlement temporairement suspendu.</td></tr>
                    <tr><td className="p-2 border border-gray-700"><span className="px-2 py-0.5 rounded bg-gray-800 text-gray-400">Annulée</span></td><td className="p-2 border border-gray-700">Enrôlement annulé (désenrôlement).</td></tr>
                  </tbody>
                </table>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">5. Rôle de chaque bouton et champ</h3>
                <ul className="space-y-2">
                  <li>
                    <strong>3 onglets</strong> :
                    <ul className="list-disc list-inside ml-4">
                      <li><em>ACS (3D Secure)</em> — authentification du porteur + enrôlement des cartes + historique.</li>
                      <li><em>EPG (Passerelle de Paiement)</em> — initiation de paiement + configuration des commerçants + historique.</li>
                      <li><em>3DSS (Serveur 3DS)</em> — sessions 3DS + annulation.</li>
                    </ul>
                  </li>
                  <li>
                    <strong>« Create Authentication »</strong> : crée une demande d'authentification
                    3-D Secure pour une transaction. Saisir l'ID de transaction, le montant et la devise.
                  </li>
                  <li>
                    <strong>« Request Challenge »</strong> : déclenche le défi 3-D Secure
                    (visible uniquement quand l'authentification est au statut « Créée »).
                  </li>
                  <li>
                    <strong>« Enroll Card » / « Enroll »</strong> : enrôle une carte auprès de
                    l'ACS pour qu'elle puisse être authentifiée en 3-D Secure.
                  </li>
                  <li>
                    <strong>« Unenroll »</strong> : désenrôle une carte (met fin à l'enrôlement).
                  </li>
                  <li>
                    <strong>« Initiate Payment »</strong> : initie un paiement via la passerelle
                    EPG pour un commerçant en ligne.
                  </li>
                  <li>
                    <strong>« Add Merchant » / « Create Merchant »</strong> : crée une
                    configuration de commerçant pour la passerelle EPG (ID, clés API, webhook).
                  </li>
                  <li>
                    <strong>« Generate Keys »</strong> : génère automatiquement une paire de
                    clés API (apiKeyHash + apiSecretHash) pour un commerçant EPG.
                  </li>
                  <li>
                    <strong>« Edit » / « Delete »</strong> : modifier ou supprimer la
                    configuration d'un commerçant EPG.
                  </li>
                  <li>
                    <strong>« Create 3DS Session »</strong> : crée une session 3-D Secure
                    pour orchestrer le flux d'authentification.
                  </li>
                  <li>
                    <strong>« Cancel »</strong> : annule une session 3DS active (rouge, visible
                    seulement si la session n'est pas encore terminée/annulée).
                  </li>
                </ul>
              </section>

              <section>
                <h3 className="text-base font-semibold text-indigo-400 mb-2">6. Questions fréquentes</h3>
                <div className="space-y-2">
                  <p><strong>Qu'est-ce que le 3-D Secure exactement ?</strong></p>
                  <p>
                    Le 3-D Secure (3DS) est un protocole d'authentification renforcée pour les
                    paiements en ligne. Il ajoute une étape de vérification où le porteur doit
                    prouver son identité (par SMS, email, code OTP, etc.) avant que la transaction
                    ne soit autorisée. C'est le mécanisme derrière les pop-ups «&nbsp;Vérification
                    par votre banque&nbsp;» sur les sites marchands.
                  </p>

                  <p><strong>C'est quoi un challenge ?</strong></p>
                  <p>
                    Le challenge est la phase active du 3-D Secure où l'émetteur demande au
                    porteur de s'authentifier (OTP par SMS/email, biométrie, mot de passe, etc.).
                    Si le challenge réussit, l'authentification est validée et la transaction
                    peut se poursuivre. Si le challenge échoue ou expire, la transaction est
                    refusée.
                  </p>

                  <p><strong>Quelle est la différence entre ACS, EPG et 3DSS ?</strong></p>
                  <p>
                    <strong>ACS</strong> = le serveur de l'émetteur qui authentifie le porteur.
                    <strong>EPG</strong> = la passerelle de paiement du commerçant (initiation,
                    autorisation, capture). <strong>3DSS</strong> = l'orchestrateur technique
                    qui fait le lien entre le marchand et l'ACS via le Directory Server (DS).
                  </p>

                  <p><strong>Faut-il enrôler une carte avant de créer une authentification ?</strong></p>
                  <p>
                    Non, l'enrôlement et l'authentification sont indépendants. L'enrôlement
                    enregistre la carte auprès de l'ACS pour un usage futur. L'authentification
                    peut être créée sans enrôlement préalable.
                  </p>

                  <p><strong>Qu'est-ce que le champ ECI ?</strong></p>
                  <p>
                    L'ECI (Electronic Commerce Indicator) est un code qui indique le niveau de
                    sécurité de l'authentification 3-D Secure : 05 = authentifié (valeur forte),
                    06 = authentification tentée ou non réalisée (valeur faible). Il est transmis
                    au commerçant pour décider d'accepter ou non la transaction.
                  </p>

                  <p><strong>Peut-on annuler une session 3DS ?</strong></p>
                  <p>
                    Oui, dans la section «&nbsp;Active Sessions&nbsp;», cliquez sur le bouton
                    «&nbsp;Cancel&nbsp;» à côté de la session. L'annulation n'est possible que
                    si la session n'est pas déjà terminée (statuts «&nbsp;Créée&nbsp;»,
                    «&nbsp;Requête envoyée&nbsp;», etc.).
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
