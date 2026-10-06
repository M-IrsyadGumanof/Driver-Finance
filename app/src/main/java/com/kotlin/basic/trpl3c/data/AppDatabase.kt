package com.kotlin.basic.trpl3c.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kotlin.basic.trpl3c.data.dao.PemasukanDao
import com.kotlin.basic.trpl3c.data.dao.PengeluaranDao
import com.kotlin.basic.trpl3c.data.dao.SaldoKategoriDao
import com.kotlin.basic.trpl3c.data.dao.TabunganDao
import com.kotlin.basic.trpl3c.data.entity.Pemasukan
import com.kotlin.basic.trpl3c.data.entity.Pengeluaran
import com.kotlin.basic.trpl3c.data.entity.SaldoKategori
import com.kotlin.basic.trpl3c.data.entity.Tabungan

/**
 * Room Database utama untuk aplikasi Driver Finance.
 * Menyimpan seluruh data finansial secara offline di perangkat lokal menggunakan SQLite.
 */
@Database(
    entities = [
        Pemasukan::class,
        Pengeluaran::class,
        Tabungan::class,
        SaldoKategori::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pemasukanDao(): PemasukanDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun tabunganDao(): TabunganDao
    abstract fun saldoKategoriDao(): SaldoKategoriDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Tambah kolom sumberDana ke tabel pengeluaran jika belum ada
                db.execSQL("ALTER TABLE pengeluaran ADD COLUMN sumberDana TEXT NOT NULL DEFAULT ''")

                // 2. Buat tabel saldo_kategori
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS saldo_kategori (
                        kategori TEXT NOT NULL PRIMARY KEY,
                        saldo INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 3. Masukkan 5 kategori default
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Tabungan', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Motor', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Bensin', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Kebutuhan Hidup', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Dana Bebas', 0)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Tambahkan kolom bensin dan pemasukanBersih ke tabel pemasukan
                db.execSQL("ALTER TABLE pemasukan ADD COLUMN bensin REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE pemasukan ADD COLUMN pemasukanBersih REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE pemasukan SET pemasukanBersih = nominal WHERE pemasukanBersih = 0.0")

                // 2. Sesuaikan kategori di saldo_kategori:
                // Hapus Bensin dari saldo kategori (karena bensin bukan saldo kategori)
                db.execSQL("DELETE FROM saldo_kategori WHERE kategori = 'Bensin'")

                // Migrasi kategori Kebutuhan Hidup ke Kehidupan Sehari-hari
                db.execSQL("UPDATE saldo_kategori SET kategori = 'Kehidupan Sehari-hari' WHERE kategori = 'Kebutuhan Hidup'")

                // Sisipkan kategori baru Keluarga dan Kehidupan Sehari-hari jika belum ada
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Kehidupan Sehari-hari', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Keluarga', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Tabungan', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Motor', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Dana Bebas', 0)")
            }
        }

        private val DATABASE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                // 5 Kategori Aktif Driver Finance:
                // Tabungan (20%), Motor (15%), Keluarga (10%), Kehidupan Sehari-hari (45%), Dana Bebas (10%)
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Tabungan', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Motor', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Keluarga', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Kehidupan Sehari-hari', 0)")
                db.execSQL("INSERT OR IGNORE INTO saldo_kategori (kategori, saldo) VALUES ('Dana Bebas', 0)")
            }
        }

        /**
         * Singleton pattern untuk memastikan hanya ada satu instance database
         * yang aktif di seluruh aplikasi demi efisiensi memori.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseConfig.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DATABASE_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
