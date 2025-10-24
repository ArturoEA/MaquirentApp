const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// Definir secretos seguros
const EMAIL_USER = defineSecret("EMAIL_USER");
const EMAIL_PASS = defineSecret("EMAIL_PASS");
const ADMIN_EMAIL = defineSecret("ADMIN_EMAIL");

/**
 * ============================================================
 * Enviar código de verificación para eliminar un grupo
 * ============================================================
 */
exports.enviarCodigoEliminacion = onCall(
  { secrets: [EMAIL_USER, EMAIL_PASS] },
  async (request) => {
    const { email, codigo, grupoNombre } = request.data;
    const uid = request.auth?.uid;

    if (!uid) throw new Error("El usuario debe estar autenticado.");
    if (!email || !codigo || !grupoNombre)
      throw new Error("Faltan datos requeridos.");

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: EMAIL_USER.value(),
        pass: EMAIL_PASS.value(),
      },
    });

    const mailOptions = {
      from: `MAQUIRENT <${EMAIL_USER.value()}>`,
      to: email,
      subject: "Código de Verificación - Eliminación de Grupo",
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; color: #333; }
            .container { max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; }
            .header { background-color: #2c3e50; color: white; padding: 20px; text-align: center; }
            .codigo { font-size: 32px; font-weight: bold; color: #e74c3c; text-align: center; margin: 20px 0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header"><h1>MAQUIRENT</h1><p>Código de Verificación</p></div>
            <p>Has solicitado eliminar el grupo electrógeno:</p>
            <h3>${grupoNombre}</h3>
            <p>Tu código de verificación es:</p>
            <div class="codigo">${codigo}</div>
            <p>Este código expira en 15 minutos.</p>
          </div>
        </body>
        </html>
      `,
    };

    try {
      await transporter.sendMail(mailOptions);
      logger.info(`Email enviado a ${email} con código ${codigo}`);
      return { success: true, message: "Email enviado correctamente" };
    } catch (error) {
      logger.error("Error enviando email:", error);
      throw new Error("Error al enviar el email");
    }
  }
);

/**
 * ============================================================
 * FUNCIÓN PROGRAMADA: Eliminar grupos expirados cada día a las 2:00 AM
 * ============================================================
 */
exports.eliminarGruposExpirados = onSchedule(
  {
    schedule: "0 2 * * *",
    timeZone: "America/Lima",
    secrets: [EMAIL_USER, EMAIL_PASS, ADMIN_EMAIL],
  },
  async () => {
    const db = admin.firestore();
    const ahora = Date.now();
    const treintaDiasEnMs = 30 * 24 * 60 * 60 * 1000;

    try {
      const snapshot = await db
        .collection("gruposElectrogenos")
        .where("eliminado", "==", true)
        .get();

      if (snapshot.empty) {
        logger.info("No hay grupos para eliminar");
        return null;
      }

      const batch = db.batch();
      const gruposEliminados = [];

      snapshot.forEach((doc) => {
        const data = doc.data();
        const fechaEliminacion = data.fechaEliminacion;

        if (fechaEliminacion && ahora - fechaEliminacion >= treintaDiasEnMs) {
          batch.delete(doc.ref);
          gruposEliminados.push({
            id: doc.id,
            codigo: data.codigo,
            fechaEliminacion: new Date(fechaEliminacion).toISOString(),
          });

          if (data.foto) eliminarFotoStorage(data.foto);
        }
      });

      await batch.commit();
      logger.info(`Eliminados ${gruposEliminados.length} grupos electrógenos`);
      await enviarNotificacionAdmin(gruposEliminados);
      return { eliminados: gruposEliminados.length };
    } catch (error) {
      logger.error("Error eliminando grupos:", error);
      return null;
    }
  }
);

/**
 * ============================================================
 * Enviar código de verificación para eliminar un usuario
 * ============================================================
 */
exports.enviarCodigoEliminacionUsuario = onCall(
  { secrets: [EMAIL_USER, EMAIL_PASS, ADMIN_EMAIL] },
  async (request) => {
    const { usuarioId, usuarioEmail } = request.data;
    const uid = request.auth?.uid;

    if (!uid) throw new Error("El usuario debe estar autenticado.");
    if (!usuarioId || !usuarioEmail)
      throw new Error("Faltan datos del usuario a eliminar.");

    const db = admin.firestore();
    const codigo = Math.floor(100000 + Math.random() * 900000).toString();

    await db.collection("codigosEliminacionUsuarios").doc(usuarioId).set({
      codigo,
      usuarioEmail,
      creado: admin.firestore.FieldValue.serverTimestamp(),
    });

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: EMAIL_USER.value(),
        pass: EMAIL_PASS.value(),
      },
    });

    const mailOptions = {
      from: `MAQUIRENT <${EMAIL_USER.value()}>`,
      to: ADMIN_EMAIL.value(),
      subject: "Código de Verificación - Eliminación de Usuario",
      html: `
        <div style="font-family: Arial; background: #f9f9f9; padding: 20px;">
          <h2>Confirmación de eliminación de usuario</h2>
          <p>Se ha solicitado eliminar al usuario:</p>
          <b>${usuarioEmail}</b>
          <p>Introduce este código para confirmar:</p>
          <div style="font-size: 32px; color: #e74c3c; text-align:center;">${codigo}</div>
          <p>Este código expira en 15 minutos.</p>
        </div>
      `,
    };

    await transporter.sendMail(mailOptions);
    logger.info(`Código enviado al admin para eliminar usuario ${usuarioEmail}`);
    return { success: true };
  }
);

/**
 * ============================================================
 * Confirmar eliminación de usuario con código
 * ============================================================
 */
exports.confirmarEliminacionUsuario = onCall(
  { secrets: [EMAIL_USER, EMAIL_PASS] },
  async (request) => {
    const { usuarioId, codigoIngresado } = request.data;

    if (!usuarioId || !codigoIngresado)
      throw new Error("Datos incompletos.");

    const db = admin.firestore();
    const docRef = db.collection("codigosEliminacionUsuarios").doc(usuarioId);
    const doc = await docRef.get();

    if (!doc.exists)
      throw new Error("No se encontró un código para este usuario.");

    const datos = doc.data();
    if (datos.codigo !== codigoIngresado)
      throw new Error("Código incorrecto.");

    // Eliminar usuario de Authentication
    await admin.auth().deleteUser(usuarioId);

    // Eliminar su documento de Firestore (colección usuarios)
    await db.collection("usuarios").doc(usuarioId).delete();

    // Eliminar el código ya usado
    await docRef.delete();

    logger.info(`Usuario ${usuarioId} eliminado correctamente.`);
    return { success: true, message: "Usuario eliminado correctamente." };
  }
);

// ------------------------------------------------------------
// FUNCIONES AUXILIARES
// ------------------------------------------------------------
async function eliminarFotoStorage(fotoUrl) {
  try {
    const bucket = admin.storage().bucket();
    const fileName = fotoUrl.split("/").pop().split("?")[0];
    const decodedFileName = decodeURIComponent(fileName);
    await bucket.file(decodedFileName).delete();
    logger.info(`Foto eliminada: ${decodedFileName}`);
  } catch (error) {
    logger.error("Error eliminando foto:", error);
  }
}

async function enviarNotificacionAdmin(gruposEliminados) {
  const adminEmail = ADMIN_EMAIL.value();
  if (!adminEmail || gruposEliminados.length === 0) return;

  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: EMAIL_USER.value(),
      pass: EMAIL_PASS.value(),
    },
  });

  const listaGrupos = gruposEliminados
    .map(
      (g) => `<li>${g.codigo} (Marcado el: ${g.fechaEliminacion})</li>`
    )
    .join("");

  const mailOptions = {
    from: `MAQUIRENT <${EMAIL_USER.value()}>`,
    to: adminEmail,
    subject: `Grupos Eliminados - ${new Date().toLocaleDateString()}`,
    html: `
      <h2>Reporte de Eliminación Automática</h2>
      <ul>${listaGrupos}</ul>
      <p>Total: ${gruposEliminados.length}</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    logger.info(`Notificación enviada al admin: ${adminEmail}`);
  } catch (error) {
    logger.error("Error enviando notificación al admin:", error);
  }
}
