# 📓 MindScribe AI — Diario Inteligente Seguros y Offline-First

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Backend-Supabase-3ECF8E?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com)
[![Gemini AI](https://img.shields.io/badge/AI-Google_Gemini-1A73E8?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev)
[![Room DB](https://img.shields.io/badge/Database-Room_SQLite-3DDC84?style=flat-square&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)

**MindScribe AI** es un diario personal, reflexivo e inteligente construido con **Kotlin** y **Jetpack Compose** bajo los lineamientos de **Material Design 3**. Está diseñado para brindar una experiencia inmersiva, de alta fidelidad estética, segura e impulsada por Inteligencia Artificial para el análisis diario de emociones y retroalimentación interactiva.

Este proyecto destaca por su arquitectura robusta **Offline-First**, permitiendo al usuario redactar, buscar y ver sus reflexiones de manera instantánea localmente con una base de datos SQLite persistida por **Room**, y sincronizar sus datos automáticamente en la nube de **Supabase** una vez restablecida la conexión.

---

## 🎨 Galería Estética y Tema Nocturno
La aplicación utiliza la paleta de colores personalizada **Slate Nocturne & Deep Teal-Black**:
*   **Fondo Ambiental**: Degradado vertical desde un verde azulado profundo (`#0D1513`) hasta un pizarra de obsidiana más denso (`#070A09`).
*   **Tarjetas y Bloques**: Contenedores en relieve con tintes esmeraldas de baja opacidad y bordes finos pulidos (`#1F3D35` al 80% de transparencia).
*   **Botones y Acentos**: Componentes interactivos resaltados con un tono menta o verde azulado vibrante, con tipografía en alto contraste para optimizar la legibilidad y evitar la fatiga visual nocturna.

---

## 🚀 Módulos y Funcionalidades del Sistema

| Módulo | Tipo de Tecnología | Descripción Funcional |
| :--- | :--- | :--- |
| **🧠 Gemini AI Insights** | Inteligencia Artificial (LLM) | Analiza el texto de tus notas en busca de emociones predominantes, hábitos implícitos, y te ofrece consejos reflexivos personalizados en tiempo real. |
| **☁️ Supabase Cloud Sync** | Backend Serverless (PostgreSQL) | Gestión de cuentas de usuario por correo electrónico/contraseña mediante **Supabase Auth** y sincronización bidireccional automática de registros. |
| **🗄️ Room Database** | Persistencia Local (SQLite) | Enfoque Offline-First completo. Todos los datos se escriben inmediatamente en base de datos Room para acceso instantáneo sin internet. |
| **🔒 Biometric Locker** | Seguridad de Hardware | Protege tus pensamientos de intrusos. Opción de bloqueo de la app que requiere reconocimiento facial o de huellas dactilares. |
| **⏰ Recordatorios Diarios** | Notificaciones del Sistema | Programador de alertas locales integrando `AlarmManager` y `BroadcastReceiver` que sobreviven al reinicio del dispositivo (`BOOT_COMPLETED`). |
| **📊 Gráficas de Estado de Ánimo** | Visualización de Datos (M3) | Panel de reportes con estadísticas visuales, análisis de tendencias y categorización de hábitos emocionales recurrentes. |

---

## 📐 Arquitectura de Software

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** robusta y limpia con flujos reactivos basados en `StateFlow` y programación asíncrona mediante **Kotlin Coroutines**.

```
com.example/
│
├── MainActivity.kt                  # Punto de entrada de la App, controlador del Lifecycle
│
├── data/                            # Capa de Acceso a Datos (Persistencia Local)
│   ├── DiaryEntry.kt                # Entidad de Datos de Room
│   ├── DiaryDao.kt                  # Objeto de Acceso a Datos (DAO)
│   ├── DiaryDatabase.kt             # Instancia e inicializador de Room DB
│   └── DiaryRepository.kt           # Repositorio (Single Source of Truth para UI)
│
├── service/                         # Capa de Integraciones y Servicios de Sistema
│   ├── SupabaseService.kt           # Servicio REST para Auth y Base de datos en la nube
│   ├── GeminiService.kt             # Cliente REST para llamadas a Gemini Pro API
│   ├── BiometricHelper.kt           # Manejo de huella digital y FaceID local
│   ├── ReminderScheduler.kt         # Gestión de AlarmManager para notificaciones
│   ├── ReminderReceiver.kt          # Receptor de Alarmas para lanzar notificaciones
│   ├── BootReceiver.kt              # Restablecedor de alarmas al reiniciar el teléfono
│   └── CalendarService.kt           # Utilidades para el manejo de fechas y calendarios
│
└── ui/                              # Capa de Presentación (Vistas de Compose y ViewModel)
    ├── DiaryViewModel.kt            # ViewModel centralizador del Estado del Diario
    ├── AuthScreen.kt                # Enrutador estético de Autenticación
    ├── LoginRegisterScreen.kt       # Pantallas de Login y Registro (Teal-Black premium UI)
    ├── JournalListScreen.kt         # Panel de Control principal y listado de entradas
    ├── JournalEntryScreen.kt        # Editor inmersivo de notas con voz y análisis IA
    ├── ReportsScreen.kt             # Informes interactivos y tendencias de emociones
    ├── SettingsScreen.kt            # Configuración de sincronización, alertas, biometría y SQL scripts
    └── theme/                       # Configuración de tipografías y colores M3
```

---

## 💾 Esquema de Base de Datos en la Nube (PostgreSQL)

Para que la sincronización en la nube funcione perfectamente en tu servidor de Supabase, ejecuta el siguiente script SQL desde la consola de administración en **SQL Editor -> New Query**:

```sql
-- =========================================================================
-- SCRIPT DE CONFIGURACIÓN COMPLETO PARA SUPABASE (MindScribe AI)
-- =========================================================================

-- 1. Crear tabla de perfiles públicos de usuarios conectados con el Auth de Supabase
create table if not exists public.profiles (
    id uuid references auth.users on delete cascade primary key,
    email text not null unique,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 2. Crear tabla de entradas de diario conectada mediante llave foránea a profiles(id)
create table if not exists public.diary_entries (
    user_id uuid not null references public.profiles(id) on delete cascade,
    date text not null,       -- Fecha de la entrada (YYYY-MM-DD)
    id_local integer,         -- ID local de Room
    title text,
    content text,
    mood text,
    tags text,
    ai_sentiment text,
    ai_advice text,
    updated_at bigint,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    primary key (user_id, date) -- Evita duplicados para el mismo usuario y fecha
);

-- 3. Desactivar RLS por simplicidad en el desarrollo (lectura/escritura pública con anon key)
alter table public.profiles disable row level security;
alter table public.diary_entries disable row level security;

-- 4. Crear un trigger que automáticamente inserte el correo y ID en public.profiles
-- cada vez que un nuevo usuario se registre en el sistema de Autenticación de Supabase.
create or replace function public.handle_new_user()
returns trigger as $$
begin
    insert into public.profiles (id, email)
    values (new.id, new.email)
    on conflict (id) do update set email = excluded.email;
    return new;
end;
$$ language plpgsql security definer;

-- 5. Vincular el trigger a la tabla auth.users
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();
```

---

## 🔐 Configuración de Variables de Entorno y Secretos

El proyecto implementa el plugin **Gradle Secrets** para inyectar llaves de API directamente en la clase `BuildConfig` de manera segura desde un archivo de variables de entorno, evitando subir secretos privados a sistemas de control de versiones.

### Configuración de Archivos Locales:

1.  Crea un archivo llamado `.env` en el directorio raíz del proyecto:
    ```bash
    touch .env
    ```
2.  Escribe tus credenciales en el archivo `.env`:
    ```env
    # Google AI Studio API Key para habilitar Gemini Pro
    GEMINI_API_KEY=AIzaSyA_tu_clave_real_de_gemini
    
    # URL pública de tu API de Supabase
    SUPABASE_URL=https://tu-id-de-proyecto.supabase.co
    
    # Llave pública anónima de acceso API de Supabase
    SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tu_clave_anon_real
    ```
3.  El archivo `.env.example` en la raíz contiene las mismas propiedades, pero configuradas con datos de demostración o placeholders genéricos para que sirvan de guía para otros desarrolladores.

---

## 🛠️ Requisitos de Compilación y Configuración de Gradle

El proyecto se gestiona mediante un Catálogo de Versiones unificado (`gradle/libs.versions.toml`) y Kotlin DSL en el sistema de dependencias:

```toml
# Archivo gradle/libs.versions.toml (Principales Dependencias)
[versions]
kotlin = "1.9.22"
agp = "8.2.2"
room = "2.6.1"
okhttp = "4.12.0"
serialization = "1.6.3"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
```

### Ejecutar Localmente:
```bash
# Sincronizar y generar archivos temporales de compilación
gradle clean assembleDebug

# Correr las pruebas unitarias locales en la máquina virtual (Robolectric)
gradle :app:testDebugUnitTest
```

---

## 💡 Flujo de Trabajo Inteligente (AI Insights)

Cuando escribes en tu diario, el sistema ejecuta un flujo continuo:
1.  **Redacción e Intención**: Puedes redactar por teclado o utilizar el dictado por voz (Speech-to-Text).
2.  **Consulta Local**: Se guarda la entrada localmente en la base de datos **Room**.
3.  **Procesamiento por Gemini**: Se envía el contenido a la API de **Gemini** con un prompt optimizado para estructurar un análisis de sentimientos y ofrecer consejos útiles y empáticos.
4.  **Respuesta Inteligente**: El resultado (emoción, hábitos e insights) se guarda localmente en la entrada y se sincroniza inmediatamente con **Supabase** si hay conexión activa.
5.  **Análisis Histórico**: El panel de **Reportes** consolida estas respuestas semanales/mensuales en métricas tangibles y gráficas interactivas.
