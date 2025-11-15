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
 * Enviar código de verificación para FINALIZAR un ALQUILER
 * ============================================================
 */
exports.enviarCodigoFinalizarAlquiler = onCall(
  { secrets: [EMAIL_USER, EMAIL_PASS] },
  async (request) => {
    const { alquilerId, nombreCliente, emailUsuario } = request.data;
    const uid = request.auth?.uid;

    if (!uid) {
      throw new Error("El usuario debe estar autenticado.");
    }
    if (!alquilerId || !nombreCliente || !emailUsuario) {
      throw new Error("Faltan datos (alquilerId, nombreCliente, emailUsuario).");
    }

    const db = admin.firestore();
    const codigo = Math.floor(100000 + Math.random() * 900000).toString();
    // Código expira en 15 minutos
    const expira = Date.now() + 15 * 60 * 1000;

    // Guardar el código en el documento del alquiler
    await db.collection("alquileresMensuales").doc(alquilerId).update({
      codigoFinalizacion: codigo,
      codigoFinalizacionExpira: expira,
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
      to: emailUsuario,
      subject: "Código de Verificación - Finalizar Alquiler",
      html: `
        <div style="font-family: Arial; padding: 20px;">
          <h2>Confirmación de Finalización de Alquiler</h2>
          <p>Has solicitado finalizar el alquiler para el cliente:</p>
          <b>${nombreCliente}</b>
          <p>Tu código de verificación es:</p>
          <div style="font-size: 32px; color: #e74c3c; text-align:center;">${codigo}</div>
          <p>Este código expira en 15 minutos.</p>
        </div>
      `,
    };

    await transporter.sendMail(mailOptions);
    logger.info(`Código enviado a ${emailUsuario} para finalizar alquiler ${alquilerId}`);
    return { success: true };
  }
);

/**
 * ============================================================
 * Confirmar finalización de alquiler con código
 * ============================================================
 */
exports.confirmarFinalizacionAlquiler = onCall(async (request) => {
  const { alquilerId, codigoIngresado } = request.data;
  const uid = request.auth?.uid;

  if (!uid) {
    throw new Error("El usuario debe estar autenticado.");
  }
  if (!alquilerId || !codigoIngresado) {
    throw new Error("Datos incompletos.");
  }

  const db = admin.firestore();
  const docRef = db.collection("alquileresMensuales").doc(alquilerId);
  const doc = await docRef.get();

  if (!doc.exists) {
    throw new Error("No se encontró el alquiler.");
  }

  const datos = doc.data();
  const { codigoFinalizacion, codigoFinalizacionExpira } = datos;

  if (!codigoFinalizacion || !codigoFinalizacionExpira) {
    throw new Error("No se ha solicitado un código para este alquiler.");
  }

  if (datos.codigoFinalizacion !== codigoIngresado) {
    throw new Error("Código incorrecto.");
  }

  if (Date.now() > datos.codigoFinalizacionExpira) {
    // Limpiar código expirado
    await docRef.update({
      codigoFinalizacion: admin.firestore.FieldValue.delete(),
      codigoFinalizacionExpira: admin.firestore.FieldValue.delete(),
    });
    throw new Error("El código ha expirado. Solicítalo nuevamente.");
  }

  // ¡Código correcto y válido!
  await docRef.update({
    finalizado: true,
    fechaFinal: new Date().toISOString(), // Opcional: Sellar fecha de finalización
    codigoFinalizacion: admin.firestore.FieldValue.delete(),
    codigoFinalizacionExpira: admin.firestore.FieldValue.delete(),
  });

  logger.info(`Alquiler ${alquilerId} finalizado por ${uid}`);
  return { success: true, message: "Alquiler finalizado correctamente." };
});
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
/**
 * ============================================================
 * FUNCIÓN PROGRAMADA: Notificar pagos pendientes (Lunes 9:00 AM)
 * ============================================================
 */
exports.notificarPagosPendientes = onSchedule({
  schedule: "50 16 * * 6", // "0 9 * * 1" = todos los lunes a las 9:00 AM
  timeZone: "America/Lima",
}, async (event) => {
  logger.info("Ejecutando notificarPagosPendientes...");
  const db = admin.firestore();
  const pagosPendientes = [];

  try {
    // 1. Obtener todos los alquileres mensuales NO finalizados
    const alquileresSnapshot = await db
      .collection("alquileresMensuales")
      .where("finalizado", "==", false)
      .get();

    if (alquileresSnapshot.empty) {
      logger.info("No hay alquileres activos. Terminando.");
      return null;
    }

    // 2. Iterar sobre cada alquiler y buscar sus detalles de mes
    for (const alquilerDoc of alquileresSnapshot.docs) {
      const alquiler = alquilerDoc.data();
      alquiler.id = alquilerDoc.id;

      const detallesSnapshot = await db
        .collection("detallesMes")
        .where("idAlquilerMensual", "==", alquiler.id)
        .get();

      for (const detalleDoc of detallesSnapshot.docs) {
        const detalle = detalleDoc.data();
        
        // 3. Comprobar si hay pagos pendientes en este detalle
        const mesPendiente = !detalle.pagoMesConfirmado;
        const hePendiente = !detalle.pagoHEConfirmado && 
                             detalle.precioHorasExtras > 0;

        if (mesPendiente || hePendiente) {
          pagosPendientes.push({
            cliente: alquiler.nombreCliente,
            periodo: detalle.tituloPeriodo,
          });
          // Solo necesitamos saber si hay al menos un pago pendiente
          // por alquiler para no saturar la lista
          break; 
        }
      }
    }

    const totalPendientes = pagosPendientes.length;
    if (totalPendientes === 0) {
      logger.info("No se encontraron pagos pendientes.");
      return null;
    }

    logger.info(`Se encontraron ${totalPendientes} alquileres con pagos pendientes.`);

    // 4. Obtener los tokens de los administradores
    const tokens = [];
    const adminSnapshot = await db
      .collection("usuarios")
      .where("rol", "==", "admin")
      .get();

    if (adminSnapshot.empty) {
      logger.warn("No se encontraron usuarios 'admin' para notificar.");
      return null;
    }

    adminSnapshot.forEach((adminDoc) => {
      const adminData = adminDoc.data();
      if (adminData.fcmTokens && Array.isArray(adminData.fcmTokens)) {
        tokens.push(...adminData.fcmTokens);
      }
    });

    if (tokens.length === 0) {
      logger.warn("Se encontraron admins, pero no tienen tokens FCM.");
      return null;
    }

    // 5. Crear y enviar el mensaje
    const mensaje = totalPendientes === 1 ?
      `Hay 1 alquiler con pagos pendientes.` :
      `Hay ${totalPendientes} alquileres con pagos pendientes.`;

    const payload = {
      notification: {
        title: "Pagos Pendientes - MAQUIRENT",
        body: mensaje,
      },
      data: {
        click_action: "FLUTTER_NOTIFICATION_CLICK",
        screen: "pagos_pendientes" // (Pantalla por crear)
      }
    };

    logger.info("Enviando notificación a tokens:", tokens);
    await admin.messaging().sendToDevice(tokens, payload);

    return { success: true, notificacionesEnviadas: tokens.length };

  } catch (error) {
    logger.error("Error al notificar pagos pendientes:", error);
    return { success: false, error: error.message };
  }
});

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
