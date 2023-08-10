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
  public ArrayList comments;
  public ArrayList subTasks;
}
static void main(String[] args) {
  def get = getConnection('https://api.ganttpro.com/v1.0/projects/')
  if (get.getResponseCode().equals(200)) {
    def list = getListData(get)
    for (i in 0..<list.size()) {
      if (list[i].name.contains("ITSM")){
        def dataProject = getTasks(list[i].projectId)
        //модуль для создания заявки и подзадач
      }
    }
  }
}

public def getTasks(def idProject){
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
    if(dataTask.parent=firstID){
      def task = generateStrList(dataTask)
      if(task!=null){
        tasks.put(dataTask.id, task)
      }
    }else{
      def task = tasks.get(dataTask.parent)
      if(dataTask!=null){
        def subTask = generateStrList(dataTask)
        if(task!=null) {
          dataTask.subTasks.add(subTask)
          dataTask.put(dataTask.id, task)
        }
      }
    }
  }

  def generateStrList(def dataTask){
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
    return task

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

def getUser(int idUser){
  def get = getConnection('https://api.ganttpro.com/v1.0/users/'+idUser)
  if (get.getResponseCode().equals(200)) {
    return getListData(get)
  }
}

def getTask(int idTask){
  def data
  def get = getConnection('https://api.ganttpro.com/v1.0/tasks/'+idTask)
  if (get.getResponseCode().equals(200)) {
    data = getListData(get)

  }

    return data
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
