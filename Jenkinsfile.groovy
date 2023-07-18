pipeline {
    agent any
    
    environment {
        ZOWE_OPT_HOST = '192.86.32.250'
        ZOWE_OPT_PORT = '10443'
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
                            credentialsId: 'COBOL',
                            usernameVariable: 'USERN',
                            passwordVariable: 'PASSW'
                        )
                    ]) {
                        env.ZOWE_OPT_USER = "${USERN}"
                        env.ZOWE_OPT_PASSWORD = "${PASSW}"
                    }
                }
                sh 'zowe daemon enable'
            }
        }

        stage('#1 Upload') {
            steps {
                script {
                    // Clone Git repo to get latest committed elements
                    git branch: 'main', url: 'https://github.com/dickywmc/DevOps.git'
                }
                // Upload COBOL program to mainframe (can be parameterized)
                sh 'zowe zos-files upload file-to-data-set "CBL0001.cbl" "Z90319.CBL(CBL0001)" --reject-unauthorized false'
            }
        }

        stage('#2 Compile COB') {
            /*when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }*/
            steps {
                script {
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "Z90319.JCL(COMPILE)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
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
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "Z90319.JCL(RUN)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
                    if (commandOutput != 'CC 0000') {
                        error "Run failure ${commandOutput}"
                    }
                }
            }
        }
    }
}