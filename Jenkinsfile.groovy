pipeline {
    agent any
    
    environment {
        //Host address and port from pipeline configuration parameters
        ZOWE_OPT_HOST = "${address}"
        ZOWE_OPT_PORT = "${port}"
    }

    tools {
        nodejs "Zowe CLI"
    }
    
    stages {
        stage('#0 Prepare credentials') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'SYST',
                            usernameVariable: 'USERN',
                            passwordVariable: 'PASSW'
                        )
                    ]) {
                        //Host ID and password from Jenkins credentials
                        env.ZOWE_OPT_USER = "${USERN}"
                        env.ZOWE_OPT_PASSWORD = "${PASSW}"
                    }
                }
                sh 'zowe daemon enable'
            }
        }

        stage('#1 Upload source from Git to mainframe') {
            steps {
                script {
                    // Retrieve the repository URL and default branch from the webhook payload
                    // Clone Git repo to get latest committed elements
                    //git branch: 'main', url: 'https://github.com/dickywmc/DevOps.git'
                    git branch: branch, url: url
                }
                // Upload COBOL program to mainframe (can be parameterized)
                sh 'zowe zos-files upload file-to-data-set "CBL0001.cbl" "WONGDIC.COB.CNTL(CBL0001)" --reject-unauthorized false'
            }
        }

        stage('#2 Compile COB') {
            /*when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }*/
            steps {
                script {
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "WONGDIC.COB.CNTL(COMPILE)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()

                    // Check compilation result
                    if (commandOutput != 'CC 0000') {
                        error "Compile failure ${commandOutput}"
                    }
                }
            }
        }

        stage('#3 Run COB') {
            /*when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }*/
            steps {
                script {
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "WONGDIC.COB.CNTL(RUN)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
                    // Check run result
                    if (commandOutput != 'CC 0000') {
                        error "Run failure ${commandOutput}"
                    }
                }
            }
        }
    }
}