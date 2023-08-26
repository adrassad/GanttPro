import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId


class Project{
  public Integer projectId;
  public String name;
  public def lastUpdate;
  public Map tasks;
}

class Task {
  public int id;
  public def createdAt;
  public String title;
  public String description;
  public def startDate;
  public def endDate;
  public def deadline
  public String responsible;
  public ArrayList<Comment> comments = new ArrayList<>();
  public ArrayList<Task> subTasks = new ArrayList();
}

class User{
  public int id;
  public String firstName;
  public String lastName;
}

class Comment{
  public int id;
  public User user;
  public def createdAt;
  public def comment;
}

static void main(String[] args) {
  Connector connector = getConnetor("69d222fbb2d84f0299f429d7a170c2a2");
  def projects = connector.getProjects()
  for (Project project:projects){
    def description = getDescriptionProject(project)
    for (def task:project.tasks){
      def descrTask = getDescriptionTask(task)
      println descrTask
    }
    println description
  }
}

Connector getConnetor(String apiKey){
  return new Connector(apiKey)

}

class Connector{

  private final String apiKey;

  Connector(String apiKey) {
    this.apiKey = apiKey
  }

  def getProjects(){
    def resources = getResources();
    ArrayList projects = new ArrayList();
    def get = getConnection('https://api.ganttpro.com/v1.0/projects/')
    if (get.getResponseCode().equals(200)) {
      def list = getListData(get)
      for (i in 0..<list.size()) {
        if (list[i].name.contains("ITSM")){
          Project project = getProject(list[i])
          project.tasks = getTasks(list[i].projectId, resources);
          projects.add(project)
        }
      }
    }
    return projects
  }

  Project getProject(def dataProject){
    Project project = new Project();
    project.projectId = dataProject.projectId;
    project.lastUpdate = parseToDate(dataProject.lastUpdate);

    project.name = dataProject.name;
    return project;
  }

  def getTasks(def idProject, def resources){
    FillerTasks fillerTask = new FillerTasks();
    fillerTask.setDataResources(resources)
    def get = getConnection('https://api.ganttpro.com/v1.0/tasks?projectId='+idProject)
    if (get.getResponseCode().equals(200)) {
      def list = getListData(get)
      def idParentNull
      for (i in 0..<list.size()) {
        if (list[i].parent==null){
          continue
        }
        fillerTask.fillTask(list[i])
      }
    }
    return fillerTask.getTasks()
  }

  def getResources(){
    Map<Integer, ArrayList> map = new HashMap<>()
    def get = getConnection('https://api.ganttpro.com/v1.0/resources')
    if (get.getResponseCode().equals(200)) {
      for (def str : getListData(get)){
        map.put(str.id, str)
      }
    }
    return map
  }

  def getUsers(){
    Map<Integer, ArrayList> map = new HashMap<>()
    def get = getConnection('https://api.ganttpro.com/v1.0/users')
    if (get.getResponseCode().equals(200)) {
      for (def str : getListData(get)){
        map.put(str.id, str)
      }
    }
    return map
  }

  def getConnection(String url){
    def get = new URL(url).openConnection();
    get.setRequestMethod("GET")
    get.setRequestProperty("Content-Type", "application/json")
    get.setRequestProperty("X-API-KEY", '69d222fbb2d84f0299f429d7a170c2a2')
    return get
  }

  def getListData(get){
    def resp = get.getInputStream().getText()
    if (resp.isEmpty()==false) {
      return new groovy.json.JsonSlurper().parseText(resp)
    }
    return new ArrayList<>()
  }


  def parseToDate(def dateString){
    DateTimeFormatter inputFormatter;
    if (dateString.length()==19){
      inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    }else if(dateString.length()==24){
      inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
    }else{
      return null;
    }
    LocalDateTime localDateTime = LocalDateTime.parse(dateString, inputFormatter);
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

}

class FillerTasks {
  public int firstID;
  public Map tasks = new HashMap();
  private Map dataResources;

  void setDataResources(Map dataResources) {
    this.dataResources = dataResources
  }

  def fillTask(def dataTask) {
    if(firstID==0){
      firstID = dataTask.parent
    }
    if(dataTask.parent==firstID){
      def task = generateTask(dataTask)
      if(task!=null){
        tasks.put(dataTask.id, task)
      }
    }else{
      def task = tasks.get(dataTask.parent)
      if(task!=null){
        def subTask = generateTask(dataTask)
        if(subTask!=null) {
          task.subTasks.add(subTask)
          tasks.put(dataTask.parent, task)
        }
      }
    }
  }

  def generateTask(def dataTask){
    boolean loadToITSM = getValueCastumFild(dataResources, dataTask, 'Выгружать в ITSM');
    if (loadToITSM==false){
      return
    }
    Task task = new Task()
    task.id = dataTask.id;
    task.createdAt = parseToDate(dataTask.createdAt);
    task.title = dataTask.name;
    task.description = dataTask.description;
    task.startDate = parseToDate(dataTask.startDate);
    task.endDate = parseToDate(dataTask.endDate);
    task.deadline = parseToDate(dataTask.deadline);
    task.responsible = getValueCastumFild(dataResources, dataTask, 'Ответственный')
    for (def dataComment:dataTask.get("comments")){
      task.comments.add(getComment(dataComment))
    }
    return task
  }

  Comment getComment(def dataComment){
    Comment comment = new Comment();
    comment.id = dataComment.id;
    comment.comment = dataComment.comment;
    comment.createdAt = dataComment.createdAt;
    comment.user = getUser(dataComment.get("user"))
    return comment
  }

  User getUser(def dataUser){
    User user = new User();
    user.id = dataUser.resourceId;
    user.firstName = dataUser.firstName;
    user.lastName = dataUser.lastName;
    return user
  }

  def getValueCastumFild(def dataResources, def task, String field){
    def value;
    if (task.get('customFields')){
      for (def str : task.get('customFields')){
        if (field==str.get('name')){
          if (str.get('value').getClass()==ArrayList.class){
            if (str.get('value').size()>0){
              value = dataResources.get(str.get('value')[0].value).name
            }else {
              value = ""
            }
          }else{
            value = str.get('value')
          }
          break
        }
      }
    }
    return value
  }

  Map getTasks() {
    return tasks
  }

  def parseToDate(def dateString){
    if(dateString==null)return
    DateTimeFormatter inputFormatter;
    if (dateString.length()==19){
      inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    }else if(dateString.length()==24){
      inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
    }else{
      return null;
    }
    LocalDateTime localDateTime = LocalDateTime.parse(dateString, inputFormatter);
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

}

def getDescriptionTask(def task){
  String description = "<ol>"
  //<div>Нужно проверит баланс и при необходимости оплатить хостинг labpack.ru</div><div>Тарифный план "Start" - 226.80 руб. в месяц</div><ol><li>Nhjkjkj<br>Написать что-то там</li><li>Второй<br>Подписать что-то там</li></ol>
  description += "<li>${task.value.title}<br>${task.value.description}"
  if (task.value.subTasks.size()>0){
    description += "<ol>"
    for (def subTask:task.value.subTasks){
      description += "<li>${task.value.title}<br>${task.value.description}"
    }
    description += "</ol>"
  }
  description+="</li>"
  description += "</ol>"
  return description
}

def getDescriptionProject(def project){
  String description = "<ol>"
  for (def task:project.tasks){
    //<div>Нужно проверит баланс и при необходимости оплатить хостинг labpack.ru</div><div>Тарифный план "Start" - 226.80 руб. в месяц</div><ol><li>Nhjkjkj<br>Написать что-то там</li><li>Второй<br>Подписать что-то там</li></ol>
    description += "<li>${task.value.title}<br>${task.value.description}"
    if (task.value.subTasks.size()>0){
      description += "<ol>"
      for (def subTask:task.value.subTasks){
        description += "<li>${task.value.title}<br>${task.value.description}"
      }
      description += "</ol>"
    }
    description+="</li>"
  }
  description += "</ol>"
  return description
}

long getLineCountByIncrement(String fileName) throws IOException {
  var lines = 0L;
  try (var reader = new BufferedReader(new FileReader(fileName))) {
    while (reader.readLine() != null) {
      lines++;
    }
    return lines;
  }
}