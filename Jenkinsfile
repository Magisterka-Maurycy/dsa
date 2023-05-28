pipeline {
    agent any

    stages {
        stage('Set Chmod') {
            steps {
                sh 'chmod 777 /var/run/docker.sock'
                sh 'chmod +x ./gradlew'
            }
        }
        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew build -Dquarkus.profile=kub'
            }
        }
        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "./gradlew sonar"
                }
            }
        }
        stage('Owasp') {
            steps {
                sh './gradlew dependencyCheckAnalyze'
            }
        }
        stage('Checkout K8S manifest SCM') {
            steps {
                checkout scmGit(branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[credentialsId: 'Maurycy_ssh', url: 'git@github.com:Magisterka-Maurycy/GitOps.git']])
            }
        }
        stage('Deploy to gitops') {
            steps {
                sshagent(['Maurycy_ssh'])
                        {
                            sh '''
                                git remote get-url origin
                                cp ./build/kubernetes/kubernetes.yml ./kubernetes/dsa/main.yaml
			            		git add ./kubernetes/
					            git config user.email "jenkins@example.com"
                                git config user.name "Jenkins"
                                git commit -m 'Jenkins Automatic Deployment - DSA'
					            git push origin HEAD:master
                            '''
                        }
            }
        }
    }
}