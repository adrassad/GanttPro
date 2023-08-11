import org.apache.groovy.json.internal.LazyMap

class Task {
  public int id;
  public def createdAt;
  public String title;
  public String description;
  public Integer parent;
  public def startDate;
  public def endDate;
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
  def get = getConnection('https://api.ganttpro.com/v1.0/projects/')
  if (get.getResponseCode().equals(200)) {
    def list = getListData(get)
    for (i in 0..<list.size()) {
      if (list[i].name.contains("ITSM")){
        def tasks = getTasks(list[i].projectId)
        for (Task task: tasks){

        }
        //модуль для создания заявки и подзадач
      }
    }
  }
}

def getTasks(def idProject){
  def dataUsers = getUsers()
  FillerTasks fillerTask = new FillerTasks();
  fillerTask.setDataResources(getResources())
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
    task.createdAt = dataTask.createdAt;
    task.title = dataTask.name;
    task.description = dataTask.description;
    task.startDate = dataTask.startDate;
    task.endDate = dataTask.endDate;
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
}

def getResources(){
  Map<Integer, LazyMap> map = new HashMap<>()
  def get = getConnection('https://api.ganttpro.com/v1.0/resources')
  if (get.getResponseCode().equals(200)) {
    for (def str : getListData(get)){
      map.put(str.id, str)
    }
  }
  return map
}

def getUsers(){
  Map<Integer, LazyMap> map = new HashMap<>()
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
