pipeline{
    agent none

    stages {
        stage('Maven Build'){
            steps{
                echo "1.Maven Build Stage"
                sh 'mvn -B clean package'
            }
        }
    }

}