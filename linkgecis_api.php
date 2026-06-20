<?php
/**
 * LinkGecis - Hostinger PHP Web API ve Yönetici Veritabanı Entegrasyonu
 * 
 * KURULUM ADIMLARI (Hostinger cPanel / hPanel):
 * 1. Hostinger Kontrol Panelinize gidin.
 * 2. "MySQL Veritabanları" (MySQL Databases) bölümünden yeni bir veritabanı, kullanıcı adı ve şifre oluşturun.
 * 3. Veritabanınıza phpMyAdmin üzerinden girin ve aşağıdaki "SQL TABLO OLUŞTURMA KODU" satırlarını SQL sekmesinde çalıştırın.
 * 4. Bu dosyadaki (api.php) $db_host, $db_name, $db_user ve $db_pass ayarlarını kendi veritabanı bilgilerinize göre düzenleyin.
 * 5. Bu dosyayı Hostinger Dosya Yöneticisi (File Manager) aracılığıyla "public_html" altına (Örn: public_html/api.php) yükleyin.
 * 6. Android uygulamasında "Yönetici Kontrol Paneli -> API & Bulut Ayarları" kısmına "https://siteniz.com/api.php" adresini girin.
 * 
 * ==========================================
 * VERİTABANI SQL TABLO OLUŞTURMA KODU (SQL):
 * ==========================================
 * 
 * CREATE TABLE IF NOT EXISTS `link_buttons` (
 *   `id` INT AUTO_INCREMENT PRIMARY KEY,
 *   `label` VARCHAR(255) NOT NULL,
 *   `target_url` TEXT NOT NULL,
 *   `popup_title` VARCHAR(255) NOT NULL,
 *   `popup_message` TEXT NOT NULL,
 *   `popup_image_url` VARCHAR(100) DEFAULT '',
 *   `countdown_seconds` INT DEFAULT 5,
 *   `click_count` INT DEFAULT 0
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 * 
 */

// Hata Raporlama (Geliştirme aşamasında açık, canlıda kapatılabilir)
error_reporting(E_ALL);
ini_set('display_errors', 1);

// JSON Çıktı başlığı ve UTF-8 yapılandırması
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// --- GÜVENLİK ANAHTARI (API KEY) ---
// Android uygulamanızla bu sunucu arasındaki veri güvenliğini sağlamak için bir şifre belirleyin.
// Varsayılan: "default_secret_key" (Uygulama ayarlarından bunu değiştirebilirsiniz)
define("SECRET_API_KEY", "default_secret_key");

// --- HOSTINGER MYSQL VERİTABANI AYARLARI ---
$db_host = "localhost";           // Hostinger'da genellikle "localhost" veya size verilen DB sunucu adresi kalır.
$db_name = "u123456789_linkdb";   // Oluşturduğunuz veritabanı adı
$db_user = "u123456789_user";     // Oluşturduğunuz veritabanı kullanıcısı
$db_pass = "Sifreniz123.";        // Veritabanı şifresi

// Veritabanı Bağlantısı
try {
    $pdo = new PDO("mysql:host=$db_host;dbname=$db_name;charset=utf8mb4", $db_user, $db_pass, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::Transient_Transaction    => true
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Veritabanı bağlantı hatası: " . $e->getMessage()
    ]);
    exit();
}

// Request parametrelerini alma (GET veya POST)
$action = isset($_REQUEST['action']) ? trim($_REQUEST['action']) : '';
$api_key = isset($_REQUEST['api_key']) ? trim($_REQUEST['api_key']) : '';

// Güvenlik doğrulaması (Sadece veri çekerken veya eklerken doğrulanacak)
if ($api_key !== SECRET_API_KEY) {
    echo json_encode([
        "success" => false,
        "message" => "Yetkisiz Erişim! Geçersiz API Şifresi."
    ]);
    exit();
}

// API İşlemleri Yönetimi
switch ($action) {
    case 'test':
        // Bağlantı test isteği
        echo json_encode([
            "success" => true,
            "message" => "Hostinger sunucu ve veritabanı bağlantısı SÜPER DURUMDA! API anahtarı onaylandı."
        ]);
        break;

    case 'get_buttons':
        // Tüm buton listesini getir
        try {
            $stmt = $pdo->query("SELECT * FROM link_buttons ORDER BY id ASC");
            $buttons = $stmt->fetchAll();
            echo json_encode([
                "success" => true,
                "data" => $buttons
            ]);
        } catch (Exception $e) {
            echo json_encode([
                "success" => false,
                "message" => "Veri çekme hatası: " . $e->getMessage()
            ]);
        }
        break;

    case 'add_button':
        // Yeni buton ve popup reklamı ekleme
        $label = isset($_POST['label']) ? trim($_POST['label']) : '';
        $target_url = isset($_POST['target_url']) ? trim($_POST['target_url']) : '';
        $popup_title = isset($_POST['popup_title']) ? trim($_POST['popup_title']) : '';
        $popup_message = isset($_POST['popup_message']) ? trim($_POST['popup_message']) : '';
        $countdown_seconds = isset($_POST['countdown_seconds']) ? intval($_POST['countdown_seconds']) : 5;

        if (empty($label) || empty($target_url) || empty($popup_title) || empty($popup_message)) {
            echo json_encode([
                "success" => false,
                "message" => "Eksik parametre! Lütfen tüm alanları doldurun."
            ]);
            exit();
        }

        try {
            $stmt = $pdo->prepare("INSERT INTO link_buttons (label, target_url, popup_title, popup_message, countdown_seconds, click_count) VALUES (?, ?, ?, ?, ?, 0)");
            $stmt->execute([$label, $target_url, $popup_title, $popup_message, $countdown_seconds]);
            echo json_encode([
                "success" => true,
                "message" => "Buton başarıyla eklendi!"
            ]);
        } catch (Exception $e) {
            echo json_encode([
                "success" => false,
                "message" => "Ekleme hatası: " . $e->getMessage()
            ]);
        }
        break;

    case 'delete_button':
        // Buton silme
        $id = isset($_POST['id']) ? intval($_POST['id']) : 0;

        if ($id <= 0) {
            echo json_encode([
                "success" => false,
                "message" => "Geçersiz ID"
            ]);
            exit();
        }

        try {
            $stmt = $pdo->prepare("DELETE FROM link_buttons WHERE id = ?");
            $stmt->execute([$id]);
            echo json_encode([
                "success" => true,
                "message" => "Buton başarıyla silindi!"
            ]);
        } catch (Exception $e) {
            echo json_encode([
                "success" => false,
                "message" => "Silme hatası: " . $e->getMessage()
            ]);
        }
        break;

    case 'click_button':
        // Tıklama sayacı artırma
        $id = isset($_POST['id']) ? intval($_POST['id']) : 0;

        if ($id <= 0) {
            echo json_encode([
                "success" => false,
                "message" => "Geçersiz ID"
            ]);
            exit();
        }

        try {
            $stmt = $pdo->prepare("UPDATE link_buttons SET click_count = click_count + 1 WHERE id = ?");
            $stmt->execute([$id]);
            echo json_encode([
                "success" => true,
                "message" => "Tıklama kaydedildi"
            ]);
        } catch (Exception $e) {
            echo json_encode([
                "success" => false,
                "message" => "Güncelleme hatası: " . $e->getMessage()
            ]);
        }
        break;

    default:
        // Geçersiz aksiyon
        echo json_encode([
            "success" => false,
            "message" => "Geçersiz işlem veya yönlendirme adresi!"
        ]);
        break;
}
?>
