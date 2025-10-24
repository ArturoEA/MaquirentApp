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
 * Function HTTPS: Enviar código de verificación por email
 */
exports.enviarCodigoEliminacion = onCall(
  { secrets: [EMAIL_USER, EMAIL_PASS] }, // <--- importante
  async (request) => {
    const { email, codigo, grupoNombre } = request.data;
    const uid = request.auth?.uid;

    if (!uid) {
      throw new Error("El usuario debe estar autenticado.");
    }
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
 * Function programada: Eliminar grupos expirados cada día a las 2:00 AM
 */
exports.eliminarGruposExpirados = onSchedule(
  {
    schedule: "0 2 * * *",
    timeZone: "America/Lima",
    secrets: [EMAIL_USER, EMAIL_PASS, ADMIN_EMAIL], // también aquí
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
