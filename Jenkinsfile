pipeline {
    agent any

    parameters {
        booleanParam(name: 'OWASP', defaultValue: false, description: 'should owasp tests be run')
        booleanParam(name: 'RELEASE', defaultValue: false, description: 'should new version be release')
    }

    stages {
        stage('Set Chmod') {
            steps {
                sh 'chmod 777 /var/run/docker.sock'
            }
        }

        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit '**/build/test-results/test/TEST*.xml'
                    jacoco(
                            execPattern: '**/build/*.exec',
                            classPattern: '**/build/classes/kotlin/main',
                            sourcePattern: '**/src/main'
                    )
                }
            }
        }

        stage('Build') {
            when {
                expression {
                    return params.RELEASE == false
                }
            }
            environment {
                QUAY_CREDS = credentials('Quay-Access')
            }
            steps {
                sh './gradlew build -Dquarkus.profile=kub -Dquarkus.container-image.username=$QUAY_CREDS_USR -Dquarkus.container-image.password=$QUAY_CREDS_PSW'
            }
        }
        stage('Release') {
            when {
                expression {
                    return params.RELEASE == true
                }
            }
            environment {
                QUAY_CREDS = credentials('Quay-Access')
            }
            steps {
                sshagent(['Maurycy_ssh'])
                        {
                            sh '''
                        ./gradlew currentVersion
                        ./gradlew release -Prelease.disableChecks -Prelease.pushTagsOnly
                        ./gradlew currentVersion 
                        ./gradlew build -Dquarkus.profile=kub -Dquarkus.container-image.username=$QUAY_CREDS_USR -Dquarkus.container-image.password=$QUAY_CREDS_PSW
                        ./gradlew markNextVersion 
                        ./gradlew currentVersion 
                        '''
                        }
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
            when {
                expression {
                    return params.OWASP == true
                }
            }
            steps {
                sh './gradlew dependencyCheckAnalyze'
            }
            post {
                always {
                    publishHTML(target: [allowMissing         : false,
                                         alwaysLinkToLastBuild: true,
                                         keepAll              : true,
                                         reportDir            : 'build/reports',
                                         reportFiles          : 'dependency-check-report.html',
                                         reportName           : 'OWASP Dependency Check',
                                         reportTitles         : 'OWASP Dependency Check']
                    )
                }
            }
        }

        stage('Deploy to gitops') {
            steps {
                checkout scmGit(branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[credentialsId: 'Maurycy_ssh', url: 'git@github.com:Magisterka-Maurycy/GitOps.git']])
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