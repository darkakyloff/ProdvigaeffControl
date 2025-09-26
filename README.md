# ProdvigaeffControl

[![Java Version](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)

**ProdvigaeffControl** — это модульная система мониторинга трудовой дисциплины сотрудников компании Продвигаефф, разработанная для автоматизации контроля и уведомления о нарушениях рабочих процессов.

## 📋 Описание

WorkGuard представляет собой Java-приложение, построенное на основе модульной архитектуры, которое автоматически отслеживает соблюдение трудовой дисциплины сотрудников и отправляет уведомления о выявленных нарушениях.

### Текущий функционал

- **Модуль контроля рабочего времени** — отслеживание своевременного заполнения рабочих часов сотрудниками
- **Интеграция с Megaplan** — получение данных о задачах и времени работы через API
- **Автоматические email-уведомления** — отправка персонализированных сообщений о нарушениях
- **Планировщик задач** — автоматический запуск проверок по расписанию (cron)

### Планируемые модули

- Отчеты по дисциплинарным нарушениям
- Мониторинг различия фактических часов работы от плановых
- Анализ ежедневных отчетов через ИИ

## 🏗️ Архитектура

Проект построен на модульной архитектуре:

```
ProdvigaeffControl/
├── core/                    # Ядро системы
│   ├── module/             # Базовые классы модулей
│   ├── scheduler/          # Планировщик задач
│   └── exception/          # Обработка исключений
├── modules/                # Конкретные модули
│   ├── worktime/          # Модуль контроля рабочего времени
│   └── cache/             # Модуль очистки кеша
├── megaplan/              # Интеграция с Megaplan API
├── http/                  # HTTP клиент
├── service/               # Сервисы (Email и др.)
├── model/                 # Модели данных
└── utils/                 # Утилиты
```

## 🚀 Быстрый старт

### Требования

- Docker и Docker Compose
- Доступ к Megaplan API
- SMTP сервер для отправки email

### Установка и запуск

1. Клонируйте репозиторий:
```bash
git clone https://github.com/darkakyloff/prodvigaeff-control.git
cd prodvigaeff-control
```

2. Настройте конфигурацию:
```bash
cp .env.example .env
```

3. Отредактируйте файл `.env` со своими настройками:
```properties
# Основные параметры Megaplan API
MEGAPLAN_API_KEY=your_api_key_here
MEGAPLAN_URL=https://yourdomain.megaplan.ru

# SMTP настройки для отправки email
SMTP_HOST=smtp.yourdomain.com
SMTP_PORT=465
SMTP_USERNAME=noreply@yourdomain.com
SMTP_PASSWORD=your_password_here

# Email настройки
EMAIL_ADMIN=admin@yourdomain.com

# HTTP клиент настройки
HTTP_MAX_RETRIES=3
HTTP_RETRY_DELAY_MS=1000
SMTP_CONNECTION_TIMEOUT=10000
SMTP_TIMEOUT=10000

# Email retry настройки
EMAIL_MAX_RETRIES=3
EMAIL_RETRY_DELAY_MS=2000

# Настройки обработки данных
MEGAPLAN_BATCH_SIZE=20
MEGAPLAN_THREAD_POOL_SIZE=5

# Логирование
APP_LOG_LEVEL=INFO
```

4. Разместите готовый JAR файл `prodvigaeff-control.jar` в корневой директории проекта

5. Запустите приложение:
```bash
docker-compose up -d
```

### Управление контейнером

```bash
# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down

# Перезапуск
docker-compose restart

# Проверка статуса
docker-compose ps
```

## ⚙️ Конфигурация

### Обязательные параметры

| Параметр | Описание |
|----------|----------|
| `MEGAPLAN_API_KEY` | API ключ для доступа к Megaplan |
| `MEGAPLAN_URL` | URL вашего Megaplan |
| `SMTP_HOST` | SMTP сервер для отправки email |
| `SMTP_PORT` | Порт SMTP сервера |
| `SMTP_USERNAME` | Логин SMTP |
| `SMTP_PASSWORD` | Пароль SMTP |

### Дополнительные параметры

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `HTTP_MAX_RETRIES` | 3 | Максимальное количество повторов HTTP запросов |
| `HTTP_RETRY_DELAY_MS` | 1000 | Задержка между повторами HTTP запросов (мс) |
| `EMAIL_MAX_RETRIES` | 3 | Максимальное количество повторов отправки email |
| `MEGAPLAN_BATCH_SIZE` | 20 | Размер пакета для обработки данных |
| `MEGAPLAN_THREAD_POOL_SIZE` | 5 | Размер пула потоков |
| `APP_LOG_LEVEL` | INFO | Уровень логирования |

## 📊 Модули

### WorktimeModule

Контролирует своевременность заполнения рабочих часов сотрудниками.

- **Расписание**: Ежедневно в 10:00
- **Функции**:
    - Проверка заполнения времени за предыдущий день
    - Отправка персонализированных уведомлений нарушителям
    - Генерация отчетов о нарушениях

### CacheCleanupModule

Очищает внутренние кеши системы для оптимизации производительности.

## 🔧 Разработка

### Добавление нового модуля

1. Создайте класс, наследующий `AbstractModule`:

```java
public class YourModule extends AbstractModule
{
    @Override
    public String getName() 
    {
        return "YourModuleName";
    }

    @Override
    public String getCronExpression() 
    {
        return "0 0 12 * * *"; // Каждый день в полдень
    }

    @Override
    public void executeModule() 
    {
        // Ваша логика
    }
}
```

2. Зарегистрируйте модуль в `WorkGuardApplication`:

```java
ModuleRegistry.register(new YourModule());
```

### Логирование

Система использует централизованное логирование через класс `Logger`:

```java
Logger.info("Информационное сообщение");
Logger.warn("Предупреждение");
Logger.error("Ошибка");
Logger.success("Успешная операция");
Logger.debug("Отладочная информация");
```

## 📈 Мониторинг

ProdvigaeffControl предоставляет подробное логирование всех операций:

- Запуск и остановка модулей
- Результаты проверок
- Статистика отправки уведомлений
- Ошибки и исключения
- Время выполнения операций

Логи сохраняются в директории `./logs` и автоматически ротируются Docker'ом.

## 🔒 Безопасность

- Все чувствительные данные хранятся в переменных окружения
- API ключи и пароли не попадают в код
- Поддержка безопасных SMTP соединений
- Graceful shutdown для корректного завершения работы

## 🐛 Устранение неполадок

### Приложение не запускается

1. Проверьте корректность настроек в `.env`
2. Убедитесь, что все обязательные параметры заданы
3. Проверьте доступность Megaplan API и SMTP сервера
4. Убедитесь, что JAR файл находится в правильном месте
5. Проверьте логи: `docker-compose logs -f`

### Email не отправляются

1. Проверьте SMTP настройки
2. Убедитесь в корректности логина/пароля
3. Проверьте настройки firewall и портов

### Ошибки при работе с Megaplan

1. Проверьте правильность API ключа
2. Убедитесь в доступности Megaplan URL
3. Проверьте права доступа API ключа
