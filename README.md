# Проект (поисковый движок) на Java Spring Framework

Проект с подключенными библиотеками springframework, mysql, jsoup, lombok,
лемматизаторами. Содержит несколько контроллеров, сервисов и репозиториев
с подключением к бд MySQL.

## Принцип работы поискового движка (кратко)

Поисковый движок представляет из себя Spring-приложение (JAR-файл, запускаемый на любом
сервере или компьютере), работающее с локально установленной базой данных MySQL, имеющее
простой веб-интерфейс и API, через который им можно управлять и получать результаты 
поисковой выдачи по запросу.
В конфигурационном файле перед запуском приложения задаются адреса сайтов, по которым
движок должен осуществлять поиск. Поисковый движок должен самостоятельно обходить все
страницы заданных сайтов и индексировать их (создавать так называемый индекс) так, чтобы
потом находить наиболее релевантные страницы по любому поисковому запросу.
Пользователь присылает запрос через API движка. Запрос — это набор слов, по которым нужно
найти страницы сайта.
Запрос определённым образом трансформируется в список слов, переведённых в базовую форму.
Например, для существительных — именительный падеж, единственное число.
В индексе ищутся страницы, на которых встречаются все эти слова.
Результаты поиска ранжируются, сортируются и отдаются пользователю.
Для вашего удобства техническая спецификация проекта собрана в отдельный документ,
находящийся `в папке проекта по адресу` **/docs/specification.rtf**.

В документе содержатся:
 - описание веб-интерфейса;
 - структура таблиц базы данных;
 - документация по командам API.

## Настройки для запуска

### Зависимости

Для успешного скачивания и подключения к проекту зависимостей
из GitHub необходимо настроить Maven конфигурацию в файле `settings.xml`.

А зависимостях, в файле `pom.xml` добавлен репозиторий для получения
jar файлов:

```xml
<repositories>
  <repository>
    <id>skillbox-gitlab</id>
    <url>https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven</url>
  </repository>
</repositories>
```

Так как для доступа требуется авторизации по токену для получения данных из
публичного репозитория, для указания токена, найдите файл `settings.xml`.

* В Windows он располагается в директории `C:/Users/<Имя вашего пользователя>/.m2`
* В Linux директория `/home/<Имя вашего пользователя>/.m2`
* В macOs по адресу `/Users/<Имя вашего пользователя>/.m2`

>**Внимание!** Актуальный токен, строка которую надо вставить в тег `<value>...</value>`
[находится в документе по ссылке](https://docs.google.com/document/d/1rb0ysFBLQltgLTvmh-ebaZfJSI7VwlFlEYT9V5_aPjc/edit?usp=sharing). 

и добавьте внутри тега `settings` текст конфигурации:

```xml
<servers>
  <server>
    <id>skillbox-gitlab</id>
    <configuration>
      <httpHeaders>
        <property>
          <name>Private-Token</name>
          <value>token</value>
        </property>
      </httpHeaders>
    </configuration>
  </server>
</servers>
```

**Не забудьте поменять токен на актуальный!**

❗️Если файла нет, то создайте `settings.xml` и вставьте в него:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
 https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>skillbox-gitlab</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>token</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>

</settings>
```

ℹ️ [Пример готового settings.xml лежит](settings.xml) в корне этого проекта.


**Не забудьте поменять токен на актуальный!**

После этого, в проекте обновите зависимости (Ctrl+Shift+O / ⌘⇧I) или
принудительно обновите данные из pom.xml. 

Для этого вызовите контекстное
у файла `pom.xml` в дереве файла проектов **Project** и выберите пункт меню **Maven -> Reload Project**.


⁉️ Если после этого у вас остается ошибка:

```text
Could not transfer artifact org.apache.lucene.morphology:morph:pom:1.5
from/to gitlab-skillbox (https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven):
authentication failed for
https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven/russianmorphology/org/apache/
    lucene/morphology/morph/1.5/morph-1.5.pom,
status: 401 Unauthorized
```

Почистите кэш Maven. Самый надежный способ, удалить директорию:

- Windows `C:\Users\<user_name>\.m2\repository`
- macOs `/Users/<user_name>/.m2/repository`
- Linux `/home/<user_name>/.m2/repository`

где `<user_name>` - имя пользователя под которым вы работаете.

После этого снова попробуйте обновить данный из `pom.xml`

### Настройки подключения к БД

В проект добавлен драйвер для подключения к БД MySQL. Для запуска проекта,
убедитесь, что у вас запущен сервер MySQL 8.x.

🐳 на сервере MySQL должна быть создана база данных serach_engine
Имя пользователя по-умолчанию `root`, настройки проекта в `src/resources/application.yml`
В файле необходимо заменить пароль (и логин) пользователя 
```yaml
spring:
  datasource:
    username: root # имя пользователя
    password: *************** # пароль пользователя
```

После этого, можете запустить проект. Если введены правильные данные,
проект успешно запуститься. Если запуск заканчивается ошибками, изучите текст
ошибок, внесите исправления и попробуйте заново.

## Структура проекта

В нашем случае будем придерживаться простой слоистой структуре (еще можно назвать "луковичная"). У нас есть
три слоя:

* **Presentation** - Наши контроллеры. слой общается с пользователями, в нашем случае ожидает запросы по API. И отдает
  ответы.
* **Business** - замый важный слой - бизнес логика, содержится в классах Сервисах. Этот слой ничего не знает о
  Presentation
  слое
* **Data Access** - слой отвечает за хранение данных, подключение к БД, реализацию запросов. Слой не знает о других
  слоях
  ничего. У нас это классы Репозитории.ный сервис, не более. Все расчеты и проверки должны быть в
классах сервисах.

  ![img.png](docs/arch.png)

Каждый слой занимается только своими задачами и работа одного слоя не должна перетекать в другой. Например, Контроллер
должен только получать данные от пользователя и вызывать нуж
Если посмотреть слои данного приложения, то увидим не только сами классы, но еще и интерфейсы между ними.
Интерфейсы нужны, чтобы слои приложения не зависели от реализаций классов. Класс LemmaController зависит от интерфейсов StatisticsService и IndexingService. Это значит, что сервисы и контроллеры могут меняться независимо, заменяться и это
не будет влиять на другие слои.
Также это относится и к передаваемым данным, у каждого слоя
может быть свои структуры для хранения данных, например Репозитории отдают Entity классы, а сервисы уже упаковывают в
нужные объекты, снова для того чтобы конечный потребитель данных не зависел напрямую от структуры данных в БД. БД может
измениться, поменять структуру таблиц - и это должно произойти максимально незаметно для других слоев.

Если еще раз посмотреть на схему, самой реализации Data Access слоя нет, у нас есть только интерфейс репозитория, а
реализация будет сгенерирована при запуске проекта.

# Описание работы поискового движка (подробно)

Проект реализован на основе Spring Boot и сборщика Maven, поэтому в файле pom.xml заданы несколько зависимостей, подключающих сам Spring Boot, шаблонизатор Thymeleaf и библиотеку Lombok, предоставляющую возможности использования удобных аннотаций.

Все классы находятся в папке src/main/java/searchengine. Это необходимо для правильной сборки и запуска приложения с помощью Maven.

Запуск приложения начинается с метода main, находящегося в классе Application, помеченном аннотацией @SpringBootApplication.
Поскольку Spring Boot включает в себя не только фреймворк Spring, но и веб-сервер Apache Tomcat, запущенное приложение сразу начинает «слушать» порт 8080 (по умолчанию) и при переходе в браузере по адресу http://localhost:8080/ начинает открываться главная страница приложения.

Сама веб-страница (файл index.html) размещена в папке resources/templates, поэтому её можно подключить в контроллере с помощью шаблонизатора Thymeleaf.

В папке controllers есть два контроллера: DefaultController и ApiController. Собственно, в DefaultController создан метод index с аннотацией @RequestMapping("/"), которая означает, что этот метод должен вызываться при запросе к главной странице приложения.
В самом методе index написан return “index”. 

Поскольку в проекте работает шаблонизатор Thymeleaf, такой код автоматически подключает и возвращает в качестве ответа код одноимённой веб-страницы (index.html), лежащей в папке resources/templates.

ApiController  формирует ответ на запросы /api/statistics, /api/startIndexing, /api/stopIndexing, /api/indexPage, /api/search.

В контроллере также созданы объекты класса StatisticsService и IndexingService (на самом деле это — интерфейсы) и в конструкторе им присваивается передаваемое значение. Это сервисы, которые отвечают за формирование ответа на запросы.

Все сервисы в приложениях на основе Spring Boot принято размещать в папке services. В ней размещены интерфейсы StatisticsService, IndexingService и классs, которые их имплементируют.

**работа StatisticsServiceImpl** 

В StatisticsServiceImpl сначала происходит подключение данных из файла конфигурации. Файл конфигурации (application.yml) лежит в папке resources приложения и содержит список сайтов с их названиями и адресами. 
Чтобы данные из файла конфигурации попали в сервис, в приложении сделано следующее:

Реализованы классы Site и SiteList. Они находятся в папке config. У них есть lombok-аннотации @Setter и @Getter, которые добавляют в классы сеттеры и геттеры для всех полей.

Класс SitesList помечен аннотациями @Component и @ConfigurationProperties(prefix = "indexing-settings"). prefix — это название ключа конфигурации, внутри которого лежит список сайтов. Аннотации приводят к автоматической инициализации объекта этого класса данными из файла application.yml.

При создании объекта класса StatisticsServiceImpl в конструктор будет передан объект класса SitesList, который, как было сказано выше, автоматически инициализируется на основе данных конфигурации.

В методе сервиса getStatistics происходит постепенная сборка объекта класса StatisticsResponse из данных о сайтах, а также некоторой случайной информации и двух заданных в начале метода массивов.

Все классы, на основе которых сервисом StatisticsServiceImpl собирается итоговый объект, размещены в папке dto и подпапке statistics. Аббревиатура DTO расшифровывается как Data Transfer Object, что в переводе с английского означает «Объект передачи данных».
Сервис 

*P.S. для сервиса IndexingService специальные классы не создавались. Все реализовывалось с помощью стандартных классов типа HashMap*

Методы StatisticsServiceImpl: getStatistic и search вызываются при запросах /statistics и /search соответственно

Метод поиска search работает по следующему алгоритму:

Поисковый запрос разбивается на отдельные слова и из этих слов формируется список уникальных лемм, исключая междометия, союзы, предлоги и частицы.
Из полученного исключаются списка леммы, которые встречаются на слишком большом количестве страниц.
Сортировать леммы в порядке увеличения частоты встречаемости (по возрастанию значения поля frequency) — от самых редких до самых частых.
По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается. Далее искать соответствия следующей леммы из этого списка страниц, а затем повторять операцию по каждой следующей лемме. Список страниц при этом на каждой итерации должен уменьшаться.
Если в итоге не осталось ни одной страницы, то выводится пустой список.
Если страницы найдены, по каждой из них расчитывается релевантность и возвращается.
Для каждой страницы рассчитыватся абсолютная релевантность — сумму всех rank всех найденных на странице лемм (из таблицы index), которая делится на максимальное значение этой абсолютной релевантности для всех найденных страниц.
Относительная релевантность получается делением абсолютной релевантности для конкретной страницы на максимальную абсолютную релевантность среди всех страниц данной поисковой выдачи.
Результат работы метода: список объектов с полями, описанными в спецификации.

**Работа IndexingServiceImpl.**

В проекте создана папка model и в ней — классы, которые будут соответствовать таблицам site и page, lemma, index в базе данных. Структура таблиц описана в технической спецификации.

В контроллере добавлен метод запуска индексации startIndexing в соответствии с технической спецификацией. В этом методе происходит 
запуск сервиса индексации сайтов.

В сервисе индексации сайтов прописан код, который будет брать из конфигурации приложения список сайтов и по каждому сайту:
 - удалять все имеющиеся данные по этому сайту (записи из таблиц site и page);
 - создавать в таблице site новую запись со статусом INDEXING;
 - обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
 - в процессе обхода постоянно обновлять дату и время в поле status_time таблицы site на текущее;
 - по завершении обхода изменять статус (поле status) на INDEXED; 
 - если произошла ошибка и обход завершить не удалось, изменять статус на FAILED и вносить в поле last_error понятную информацию о произошедшей ошибке.

Для перехода по очередной ссылке создается новый поток при помощи Fork-Join. Fork-join вызывает на выполнение рекурсивную задачу класса SiteParser extends RecursiveAction;. Поток получает содержимое страницы и перечень ссылок, которые есть на этой странице (значений атрибутов href HTML-тегов \<a\>), при помощи JSOUP.

Также сервис реализует функцию остановки обхода сайтов — команду API stopIndexing в соответствии с технической спецификацией. Она останавливает все потоки и записывает в базу данных для всех сайтов, страницы которых ещё не удалось обойти, состояние FAILED и текст ошибки «Индексация остановлена пользователем».

В рамках следующего этапа к проекту подключен так называемый лемматизатор — библиотека, которая позволяет получать леммы слов — их исходные формы. Например, для существительных — это слово в именительном падеже и единственном числе. Лемматизацию удобно использовать в поисковых движках, поскольку она позволяет искать нужную информацию с учётом морфологии.

Созданы классы ещё для двух таблиц базы данных (lemma и index) в соответствии с технической спецификацией.

HTML-код переданной веб-страницы, сохраняется в базу данных в таблицу page, преобразовывается в набор лемм и их количеств, а затем эта информация сохраняется в таблицы lemma и index базы данных следующим образом:
 - Леммы добавляются в таблицу lemma. Если леммы в таблице ещё нет, она должна туда добавляться со значением frequency, равным 1. Если же лемма в таблице уже есть, число frequency увеличивается на 1. Число frequency у каждой леммы в итоге соответствует количеству страниц, на которых эта лемма встречается хотя бы один раз.

Связки лемм и страниц добавляются в таблицу index. Для каждой пары «лемма-страница» в этой таблице создавается одна запись с указанием количества данной леммы на страницы в поле rank.


