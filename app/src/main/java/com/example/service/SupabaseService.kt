package com.example.service

import com.example.data.DiaryEntry
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    val id: String? = null,
    val user: SupabaseUser? = null
)

@JsonClass(generateAdapter = true)
data class AuthResult(
    val success: Boolean,
    val userId: String?,
    val errorMsg: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseDiaryEntry(
    val user_id: String,
    val id_local: Int,
    val date: String,
    val title: String,
    val content: String,
    val mood: String,
    val tags: String,
    val ai_sentiment: String,
    val ai_advice: String,
    val updated_at: Long
)

object SupabaseService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun extractUserId(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return try {
            val adapter = moshi.adapter(SupabaseAuthResponse::class.java)
            val response = adapter.fromJson(responseBody)
            response?.id ?: response?.user?.id
        } catch (e: Exception) {
            val regex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(responseBody)?.groupValues?.get(1)
        }
    }

    /**
     * Synchronizes a single entry to Supabase using a POST (upsert) request.
     * Returns true if successful.
     */
    suspend fun syncEntry(
        url: String,
        anonKey: String,
        userId: String,
        entry: DiaryEntry
    ): Boolean {
        if (url.isEmpty() || anonKey.isEmpty()) return false

        val sanitizedUrl = url.trim().removeSuffix("/")
        val endpoint = "$sanitizedUrl/rest/v1/diary_entries"

        // Map Room entity to Supabase format
        val supabaseEntry = SupabaseDiaryEntry(
            user_id = userId,
            id_local = entry.id,
            date = entry.date,
            title = entry.title,
            content = entry.content,
            mood = entry.mood,
            tags = entry.tags,
            ai_sentiment = entry.aiSentiment,
            ai_advice = entry.aiAdvice,
            updated_at = entry.updatedAt
        )

        val adapter = moshi.adapter(SupabaseDiaryEntry::class.java)
        val jsonBody = adapter.toJson(supabaseEntry)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Content-Type", "application/json")
            // Supabase upsert option: resolution=merge-duplicates uses primary keys
            // To make this work, the Supabase table 'diary_entries' should have 'date' as primary key or 'id_local' as primary key.
            // Sending 'Prefer: resolution=merge-duplicates' handles upserts.
            .addHeader("Prefer", "resolution=merge-duplicates")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            response.close()
            // 200 OK, 201 Created or 204 No Content are all successes
            code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Registers a user in Supabase Auth using email and password.
     * Prevents duplicate emails by checking with Supabase's built-in auth system.
     */
    suspend fun registerUser(
        url: String,
        anonKey: String,
        email: String,
        password: String
    ): AuthResult {
        if (url.isEmpty() || anonKey.isEmpty()) {
            return AuthResult(false, null, "Supabase no está configurado. Por favor, configura las credenciales.")
        }

        val sanitizedUrl = url.trim().removeSuffix("/")
        val endpoint = "$sanitizedUrl/auth/v1/signup"

        val bodyJson = """
            {
                "email": "${email.trim()}",
                "password": "${password.trim()}"
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = bodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val responseBody = response.body?.string() ?: ""
            response.close()

            if (code in 200..299) {
                val userId = extractUserId(responseBody)
                AuthResult(true, userId, null)
            } else {
                val errorMsg = if (responseBody.contains("already registered") || responseBody.contains("User already exists")) {
                    "Este correo electrónico ya está registrado. Intenta iniciar sesión."
                } else if (responseBody.contains("valid email")) {
                    "Por favor, introduce un correo electrónico válido."
                } else if (responseBody.contains("at least 6 characters")) {
                    "La contraseña debe tener al menos 6 caracteres."
                } else {
                    "Error de registro ($code). Verifica tus credenciales."
                }
                AuthResult(false, null, errorMsg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult(false, null, "Error de red al conectar con Supabase: ${e.message}")
        }
    }

    /**
     * Authenticates a user in Supabase Auth using email and password.
     */
    suspend fun loginUser(
        url: String,
        anonKey: String,
        email: String,
        password: String
    ): AuthResult {
        if (url.isEmpty() || anonKey.isEmpty()) {
            return AuthResult(false, null, "Supabase no está configurado.")
        }

        val sanitizedUrl = url.trim().removeSuffix("/")
        val endpoint = "$sanitizedUrl/auth/v1/token?grant_type=password"

        val bodyJson = """
            {
                "email": "${email.trim()}",
                "password": "${password.trim()}"
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = bodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val responseBody = response.body?.string() ?: ""
            response.close()

            if (code in 200..299) {
                val userId = extractUserId(responseBody)
                AuthResult(true, userId, null)
            } else {
                val errorMsg = if (responseBody.contains("Invalid login credentials") || responseBody.contains("invalid_credentials")) {
                    "Correo electrónico o contraseña incorrectos."
                } else if (responseBody.contains("Email not confirmed")) {
                    "Por favor, confirma tu correo electrónico en tu bandeja de entrada."
                } else {
                    "Error de inicio de sesión ($code)."
                }
                AuthResult(false, null, errorMsg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult(false, null, "Error de red al conectar con Supabase: ${e.message}")
        }
    }

    /**
     * Returns a SQL script that the user can execute in Supabase SQL Editor
     * to set up both the profiles and diary_entries tables correctly, 
     * complete with a relational foreign key and an automatic user creation trigger.
     */
    fun getSupabaseSetupSql(): String {
        return """
            -- =========================================================================
            -- SCRIPT DE CONFIGURACIÓN COMPLETO PARA SUPABASE (MindScribe AI)
            -- =========================================================================
            -- Ejecuta este script en el Editor SQL de Supabase (SQL Editor -> New Query)
            
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
        """.trimIndent()
    }
}
