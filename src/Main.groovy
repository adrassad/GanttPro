public class Task {
  int id;
  Date createdAt;
  String name;
  int ownerId;
  Date startDate;
  Date endDate;

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
  Map<Integer, ArrayList> listTasks = HashMap;
  def get = getConnection('https://api.ganttpro.com/v1.0/tasks?projectId='+idProject)
  if (get.getResponseCode().equals(200)) {
    def list = getListData(get)
    for (i in 0..<list.size()) {
      def task = list[i]

    }
  }
  return null
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