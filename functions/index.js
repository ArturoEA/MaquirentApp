const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const { onDocumentCreated, onDocumentDeleted, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { getStorage } = require("firebase-admin/storage")

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

exports.borrarFotosDeGrupoEliminado = onDocumentUpdated("gruposElectrogenos/{grupoId}", async (event) => {
  const antes = event.data.before.data();
  const despues = event.data.after.data();

  // Si el campo 'eliminado' cambió de 'false' a 'true'
  if (antes.eliminado === false && despues.eliminado === true) {
    logger.info(`Iniciando borrado de fotos para Grupo (Soft Delete): ${event.params.grupoId}`);
    
    const db = admin.firestore();
    const storage = getStorage();
    const grupoId = event.params.grupoId;

    try {
      // 1. Borrar foto principal del grupo
      if (despues.foto && despues.foto.includes("firebasestorage")) {
        await borrarFotoPorUrl(storage, despues.foto, "Foto Principal");
      }

      // 2. Buscar todos los mantenimientos de ESE grupo
      const mantenimientosSnapshot = await db.collection("mantenimientos")
        .where("idGrupo", "==", grupoId)
        .get();

      if (mantenimientosSnapshot.empty) {
        logger.info("No hay mantenimientos, borrado de fotos terminado.");
        return null;
      }
      
      // 3. Borrar todas las fotos de cada mantenimiento
      let contadorFotos = 0;
      for (const doc of mantenimientosSnapshot.docs) {
        const mantenimiento = doc.data();
        if (mantenimiento.fotosUrls && Array.isArray(mantenimiento.fotosUrls)) {
          for (const url of mantenimiento.fotosUrls) {
            await borrarFotoPorUrl(storage, url, "Foto Mantenimiento");
            contadorFotos++;
          }
          await doc.ref.update({ fotosUrls: [] });
        }
      }
      
      logger.info(`Borrado de fotos completado. Total: ${contadorFotos} fotos de mant.`);
      return { success: true, fotosBorradas: contadorFotos };

    } catch (error) {
      logger.error(`Error borrando fotos para ${grupoId}:`, error);
      return { success: false };
    }
  }
  return null;
});

exports.limpiarDatosGrupoEliminado = onDocumentDeleted("gruposElectrogenos/{grupoId}", async (event) => {
  const grupoId = event.params.grupoId;
  const db = admin.firestore();
  
  logger.info(`INICIANDO LIMPIEZA EN CASCADA (Hard Delete) para Grupo: ${grupoId}`);

  // 1. Borrar Alquileres Mensuales
  const alquileresMensuales = await db.collection("alquileresMensuales")
    .where("idGrupo", "==", grupoId).get();
    
  if (!alquileresMensuales.empty) {
    logger.info(`Borrando ${alquileresMensuales.size} alquiler(es) mensual(es)`);
    for (const doc of alquileresMensuales.docs) {
      await doc.ref.delete();
    }
  }

  // 2. Borrar Mantenimientos
  const mantenimientos = await db.collection("mantenimientos")
    .where("idGrupo", "==", grupoId).get();
    
  if (!mantenimientos.empty) {
    const batchManto = db.batch();
    mantenimientos.forEach(doc => batchManto.delete(doc.ref));
    await batchManto.commit();
  }
  
  // ... (Añadir borrado de AlquileresDiarios si es necesario) ...
  
  logger.info(`LIMPIEZA COMPLETA (Hard Delete) para Grupo: ${grupoId}`);
  return { success: true };
});
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
  const { alquilerId, codigoIngresado, horometroFinal, fechaFinal } = request.data;
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

  await docRef.update({
    finalizado: true,
    fechaFinal: fechaFinal, 
    horometroFinal: horometroFinal,
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
  schedule: "0 9 * * 1",
  timeZone: "America/Lima",
  region: "us-central1",
}, async (event) => {
  logger.info("Ejecutando notificarPagosPendientes...");
  const db = admin.firestore();
  const pagosPendientes = [];

  try {
    // 1. Obtener alquileres activos
    const alquileresSnapshot = await db
      .collection("alquileresMensuales")
      .where("finalizado", "==", false)
      .get();
      
    if (alquileresSnapshot.empty) {
      logger.info("No hay alquileres activos.");
      return null;
    }

    // 2. Buscar pagos pendientes
    let totalPendientes = 0;
    
    for (const alquilerDoc of alquileresSnapshot.docs) {
      const alquiler = alquilerDoc.data();
      
      // Obtener detalles de mes para este alquiler
      const detallesSnapshot = await db
        .collection("detallesMes")
        .where("idAlquilerMensual", "==", alquilerDoc.id)
        .get();
      
      detallesSnapshot.forEach((detalleDoc) => {
        const detalle = detalleDoc.data();
        
        // Verificar si hay pagos pendientes
        if (!detalle.pagoMesConfirmado || 
            (detalle.horasExtras > 0 && !detalle.pagoHEConfirmado)) {
          pagosPendientes.push({
            alquilerId: alquilerDoc.id,
            empresa: alquiler.nombreCliente,
            periodo: detalle.tituloPeriodo,
            pagoMesPendiente: !detalle.pagoMesConfirmado,
            pagoHEPendiente: detalle.horasExtras > 0 && !detalle.pagoHEConfirmado
          });
          totalPendientes++;
        }
      });
    }

    if (totalPendientes === 0) {
      logger.info("No hay pagos pendientes.");
      return null;
    }

    logger.info(`Se encontraron ${totalPendientes} alquileres con pagos pendientes.`);

    // 3. Obtener tokens de administradores
    const tokens = [];
    const adminSnapshot = await db
      .collection("usuarios")
      .where("rol", "==", "admin")
      .get();
      
    adminSnapshot.forEach((adminDoc) => {
      const adminData = adminDoc.data();
      if (adminData.fcmTokens && Array.isArray(adminData.fcmTokens)) {
        tokens.push(...adminData.fcmTokens);
      }
    });

    if (tokens.length === 0) {
      logger.warn("No se encontraron tokens FCM de administradores.");
      return null;
    }

    logger.info(`Enviando notificación a ${tokens.length} token(s)`);

    // 4.Usar sendEach() en lugar de sendToDevice()
    const messages = tokens.map(token => ({
      token: token,
      notification: {
        title: "Pagos Pendientes - MAQUIRENT",
        body: `Hay ${totalPendientes} alquiler${totalPendientes > 1 ? 'es' : ''} con pagos pendientes.`
      },
      data: {
        type: "pagos_pendientes",
        count: totalPendientes.toString(),
        timestamp: Date.now().toString()
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
          channelId: "pagos_pendientes"
        }
      },
      apns: {
        payload: {
          aps: {
            sound: "default",
            badge: totalPendientes
          }
        }
      }
    }));

    const response = await admin.messaging().sendEach(messages);

    let successCount = 0;
    let failureCount = 0;
    const failedTokens = [];

    response.responses.forEach((resp, idx) => {
      if (resp.success) {
        successCount++;
        logger.info(`Mensaje enviado exitosamente al token: ${tokens[idx].substring(0, 10)}...`);
      } else {
        failureCount++;
        logger.error(`Error al enviar al token ${tokens[idx].substring(0, 10)}...: ${resp.error?.message}`);
        
        // Si el token es inválido, marcarlo para eliminación
        if (resp.error?.code === 'messaging/invalid-registration-token' ||
            resp.error?.code === 'messaging/registration-token-not-registered') {
          failedTokens.push(tokens[idx]);
        }
      }
    });

    logger.info(`Notificaciones enviadas: ${successCount} exitosas, ${failureCount} fallidas`);

    // Opcional: Limpiar tokens inválidos
    if (failedTokens.length > 0) {
      await limpiarTokensInvalidos(db, failedTokens);
    }

    return { 
      success: true, 
      totalPendientes,
      notificacionesEnviadas: successCount,
      notificacionesFallidas: failureCount
    };

  } catch (error) {
    logger.error("Error al notificar pagos pendientes:", error);
    return { success: false, error: error.message };
  }
});

exports.notificarNuevoAlquilerMensual = onDocumentCreated("alquileresMensuales/{alquilerId}", async (event) => {
  const db = admin.firestore();

  // 1. Obtener los datos del alquiler recién creado
  const snap = event.data;
  if (!snap) {
    logger.warn("No hay datos en el evento de creación.");
    return null;
  }
  const alquiler = snap.data();
  const creadorUid = alquiler.adminUid; // El UID del admin que lo creó
  const nombreCliente = alquiler.nombreCliente || "Cliente Desconocido";

  logger.info(`Nuevo alquiler mensual creado por ${creadorUid} para ${nombreCliente}.`);

  // 2. Obtener tokens de TODOS los administradores
  const tokens = [];
  const adminSnapshot = await db
    .collection("usuarios")
    .where("rol", "==", "admin")
    .get();

  if (adminSnapshot.empty) {
    logger.warn("No se encontraron admins para notificar.");
    return null;
  }
  
  const tokensAExcluir = [];

  adminSnapshot.forEach((adminDoc) => {
    // Si el admin es el que creó el alquiler, NO le notificamos
    if (adminDoc.id === creadorUid) {
      logger.info(`Excluyendo al creador de la notificación: ${adminDoc.id}`);
      return;
    }
    
    const adminData = adminDoc.data();
    if (adminData.fcmTokens && Array.isArray(adminData.fcmTokens)) {
      tokens.push(...adminData.fcmTokens);
    }
  });

  if (tokens.length === 0) {
    logger.info("No hay otros admins a quienes notificar.");
    return null;
  }

  // 3. Crear y enviar las notificaciones
  logger.info(`Enviando notificación de nuevo alquiler a ${tokens.length} token(s)`);

  const messages = tokens.map(token => ({
    token: token,
    notification: {
      title: "Nuevo Alquiler Registrado",
      body: `Se creó un nuevo alquiler mensual para: ${nombreCliente}`
    },
    data: {
      type: "nuevo_alquiler",
      alquilerId: snap.id
    },
    android: {
      priority: "high",
      notification: {
        sound: "default",
        channelId: "pagos_pendientes"
      }
    },
    apns: {
      payload: { aps: { sound: "default", badge: 1 } }
    }
  }));

  const response = await admin.messaging().sendEach(messages);

  // 4.Limpiar tokens fallidos
  let failureCount = 0;
  const failedTokens = [];

  response.responses.forEach((resp, idx) => {
    if (!resp.success) {
      failureCount++;
      if (resp.error?.code === 'messaging/invalid-registration-token' ||
          resp.error?.code === 'messaging/registration-token-not-registered') {
        failedTokens.push(tokens[idx]);
      }
    }
  });

  if (failureCount > 0) {
    logger.warn(`Fallaron ${failureCount} notificaciones de nuevo alquiler.`);
  }

  if (failedTokens.length > 0) {
    await limpiarTokensInvalidos(db, failedTokens);
  }

  return { 
    success: true, 
    notificacionesEnviadas: response.successCount 
  };
});

// Función auxiliar para limpiar tokens inválidos
async function limpiarTokensInvalidos(db, tokensInvalidos) {
  logger.info(`Limpiando ${tokensInvalidos.length} token(s) inválido(s)...`);
  
  try {
    const adminSnapshot = await db
      .collection("usuarios")
      .where("rol", "==", "admin")
      .get();
    
    const batch = db.batch();
    let cleanedCount = 0;

    adminSnapshot.forEach((adminDoc) => {
      const adminData = adminDoc.data();
      if (adminData.fcmTokens && Array.isArray(adminData.fcmTokens)) {
        const tokensActualizados = adminData.fcmTokens.filter(
          token => !tokensInvalidos.includes(token)
        );
        
        if (tokensActualizados.length !== adminData.fcmTokens.length) {
          batch.update(adminDoc.ref, { fcmTokens: tokensActualizados });
          cleanedCount++;
        }
      }
    });

    if (cleanedCount > 0) {
      await batch.commit();
      logger.info(`Se limpiaron tokens inválidos de ${cleanedCount} usuario(s)`);
    }
  } catch (error) {
    logger.error("Error al limpiar tokens inválidos:", error);
  }
}
/**
 * ============================================================
 * TRIGGER: Limpiar datos asociados al eliminar un Alquiler Mensual
 * ============================================================
 */
exports.limpiarDatosAlquilerMensualEliminado = onDocumentDeleted("alquileresMensuales/{alquilerId}", async (event) => {
  const alquilerId = event.params.alquilerId;
  const db = admin.firestore();
  const batch = db.batch();

  logger.info(`Iniciando limpieza para alquiler mensual eliminado: ${alquilerId}`);

  try {
    // 1. Encontrar y borrar todos los 'detallesMes' asociados
    const detallesSnapshot = await db.collection("detallesMes")
      .where("idAlquilerMensual", "==", alquilerId)
      .get();

    if (!detallesSnapshot.empty) {
      detallesSnapshot.forEach(doc => {
        logger.info(`Borrando detalleMes: ${doc.id}`);
        batch.delete(doc.ref);
      });
    }

    // 2. Encontrar y borrar todos los 'ingresosRegistrados' asociados
    const ingresosSnapshot = await db.collection("ingresosRegistrados")
      .where("idAlquiler", "==", alquilerId)
      .get();

    if (!ingresosSnapshot.empty) {
      ingresosSnapshot.forEach(doc => {
        logger.info(`Borrando ingresoRegistrado: ${doc.id}`);
        batch.delete(doc.ref);
      });
    }

    // 3. Ejecutar el borrado en lote
    await batch.commit();
    logger.info(`Limpieza completa para alquiler ${alquilerId}.`);
    return { success: true };

  } catch (error) {
    logger.error(`Error limpiando datos para alquiler ${alquilerId}:`, error);
    return { success: false, error: error.message };
  }
});
/**
 * ============================================================
 * TRIGGER: Notificar a admins cuando se crea un Alquiler DIARIO
 * ============================================================
 */
exports.notificarNuevoAlquilerDiario = onDocumentCreated("alquileresDiarios/{alquilerId}", async (event) => {
  const db = admin.firestore();
  const snap = event.data;
  if (!snap) {
    logger.warn("No hay datos en el evento de creación de Alquiler Diario.");
    return null;
  }
  
  const alquiler = snap.data();
  const creadorUid = alquiler.adminUid;
  const nombreCliente = alquiler.nombreCliente || "Cliente Desconocido";

  logger.info(`Nuevo alquiler diario creado por ${creadorUid} para ${nombreCliente}.`);

  // 2. Obtener tokens de TODOS los administradores
  const tokens = [];
  const adminSnapshot = await db.collection("usuarios")
    .where("rol", "==", "admin").get();

  if (adminSnapshot.empty) {
    logger.warn("No se encontraron admins para notificar.");
    return null;
  }
  
  adminSnapshot.forEach((adminDoc) => {
    if (adminDoc.id === creadorUid) {
      return; 
    }
    
    const adminData = adminDoc.data();
    if (adminData.fcmTokens && Array.isArray(adminData.fcmTokens)) {
      tokens.push(...adminData.fcmTokens);
    }
  });

  if (tokens.length === 0) {
    logger.info("No hay otros admins a quienes notificar.");
    return null;
  }

  // 3. Crear y enviar las notificaciones
  const messages = tokens.map(token => ({
    token: token,
    notification: {
      title: "Nuevo Alquiler Diario",
      body: `Se registró un alquiler diario para: ${nombreCliente}`
    },
    data: { type: "nuevo_alquiler_diario", alquilerId: snap.id },
    android: { priority: "high", notification: { channelId: "pagos_pendientes" }},
    apns: { payload: { aps: { sound: "default", badge: 1 } } }
  }));

  const response = await admin.messaging().sendEach(messages);
  
  // (Opcional: Limpiar tokens fallidos reusando tu función 'limpiarTokensInvalidos')
  
  logger.info(`Notificaciones de alquiler diario enviadas: ${response.successCount}`);
  return { success: true, notificacionesEnviadas: response.successCount };
});

/**
 * ============================================================
 * TRIGGER: Limpiar ingresos si se elimina un Alquiler DIARIO
 * ============================================================
 */
exports.limpiarDatosAlquilerDiarioEliminado = onDocumentDeleted("alquileresDiarios/{alquilerId}", async (event) => {
  const alquilerId = event.params.alquilerId;
  const db = admin.firestore();
  const batch = db.batch();

  logger.info(`Iniciando limpieza de ingresos para alquiler diario: ${alquilerId}`);

  try {
    const ingresosSnapshot = await db.collection("ingresosRegistrados")
      .where("idAlquiler", "==", alquilerId)
      .get();

    if (!ingresosSnapshot.empty) {
      ingresosSnapshot.forEach(doc => {
        logger.info(`Borrando ingresoRegistrado: ${doc.id}`);
        batch.delete(doc.ref);
      });
    }

    await batch.commit();
    logger.info(`Limpieza de ingresos completa para alquiler diario ${alquilerId}.`);
    return { success: true };

  } catch (error) {
    logger.error(`Error limpiando ingresos para alquiler diario ${alquilerId}:`, error);
    return { success: false, error: error.message };
  }
});






// ------------------------------------------------------------
// FUNCIONES AUXILIARES
// ------------------------------------------------------------
async function borrarFotoPorUrl(storage, url, logTipo) {
  try {
    const urlObj = new URL(url);
    const pathName = urlObj.pathname;
    const filePath = decodeURIComponent(pathName.split("/o/")[1].split("?")[0]);
    
    const file = storage.bucket().file(filePath);
    await file.delete();
    logger.info(`(${logTipo}) Foto borrada de Storage: ${filePath}`);
  } catch (err) {
    logger.warn(`No se pudo borrar foto (${logTipo}) de Storage: ${url}. Error: ${err.message}`);
  }
}